# supply-chain-service

供应链协同（ADR-019，对应 docs/12 文档五/六）。v1 聚焦**库存 + BOM 自动核减**：
仓库/SkuStock（条件更新防负）、菜品 BOM，消费 `DiningChecked` 按 BOM 核减门店库存。

## 端口与依赖

- HTTP 端口：8091
- 数据库：`supply_chain_db`
- RocketMQ：消费 `DiningChecked`（dining-pos 发布）
- Flyway：`V1__init.sql`（warehouse / sku_stock / dish_bom / processed_event）

## 对外接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/supply/stocks?warehouse_id=&sku_id=` | 查库存 |
| POST | `/admin/v1/warehouses` | 建仓（HQ/CENTRAL_KITCHEN/STORE） |
| GET | `/admin/v1/warehouses` | 仓库列表 |
| POST | `/admin/v1/stocks` | 设置库存（按 warehouse+sku 幂等） |
| POST | `/admin/v1/boms` | 设置菜品 BOM（按 output_sku 幂等） |

错误码 `SUPPLY_*`：WAREHOUSE_NOT_FOUND、STOCK_NOT_FOUND、INSUFFICIENT_STOCK。

## 消费事件 + BOM 核减（12 文档 6.3）

| event_type | topic | consumer_group | 动作 |
|---|---|---|---|
| `DiningChecked` | `dining.dining-checked` | `supply-chain-dining-checked-consumer` | 据 merchant_id 定位门店仓 → 逐 SERVED 行按 BOM 核减门店库存 |

- 核减量 = `material.quantity × 销量 / bom.output_quantity`。
- **允许临时为负**（12 文档 5.6/6.3，`SkuStockMapper.deductAllowNegative`）；手动出库则走条件更新防负
  （`deductConditional`，`WHERE quantity >= delta`，同 ADR-018）。
- 未配置门店仓/BOM/库存记录仅记录异常日志（待库管补录后重放），不抛出以免无意义重投。
- 幂等：`processed_event (consumer_group, event_id)`，ORDERLY。

## v1 范围与简化

1. `sku_stock` 用代理主键 `stock_id` + 唯一键 `(warehouse_id, sku_id)`（偏离 10 文档复合主键，MP 支持弱）。
2. **未实现**：采购审批 PurchaseOrder、门店订货/调拨 StoreOrderRequest/TransferOrder、中央厨房 ProcessingOrder、
   无纸化领料 RequisitionForm、盘点 StockTakingRecord、成本核算 CostSnapshot、库存预警事件。
3. BOM 成本价/加权平均未计（成本核算后续）。

## 多租户调试

调用带 `X-Tenant-Id` 头；消费事件按 envelope 的 `tenant_id` 设置 `TenantContext`。

## 范围边界

仅供应链库存/BOM。前厅收银在 dining-pos-service；不跨模块访问表（ADR-005）。
