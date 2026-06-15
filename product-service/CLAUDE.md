# product-service

商品中心（门票全链路，对应 docs/07/09/10 文档）。负责门票产品 `ticket_product` 与票种 `ticket_sku`
的主数据管理，是订单创建链路中第一个被调用的服务（`order-service` 通过
`GET /api/v1/skus/{sku_id}` 校验票种状态与规则）。

## 端口与数据库

- HTTP 端口：8081
- 数据库：`product_db`（库已在 `docker/mysql/init/01-init-databases.sql` 中创建）
- Flyway：`V1__init.sql`（表结构，见 docs/10 §2.1/2.2）、`V2__seed.sql`（黄金路径示例数据，
  与 docs/09 文档示例一致：`product_id=9001`/`sku_id=1001`/`tenant_id=1001`/`merchant_id=2001`）

## 对外接口（docs/09 文档二）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/skus/{sku_id}` | 查询票种信息：状态、票种类型、是否需要场次、实名制规则 |

返回格式为 `com.qmp.kernel.common.ApiResponse`（`{code, message, data, trace_id}`）。
SKU 查询返回含 `refund_policy`（产品级退改签规则快照），供 order 下单落 `order_item.refund_policy_snapshot`。

> **退改签规则按产品配置**：`ticket_product.refund_policy`（JSON，V3 迁移新增）由后台
> `POST /admin/v1/products` 的 `refund_policy` 字段设置（每景区可为不同产品设不同规则）。
> 形如 `{"type":"TIERED","cutoff_hours":24,"refund_ratio":0.8}`，`NONE` 表示不可退。

## 目录结构

- `entity`：`TicketProduct`/`TicketSku`（MyBatis-Plus `@TableName`，主键为雪花 ID `@TableId(ASSIGN_ID)`）
- `mapper`：`BaseMapper` 子接口
- `service`：`SkuQueryService`（联表查询 + 解析 `time_slot_definitions` JSON）
- `controller`：`SkuController`
- `error`：`ProductErrorCode`（`PRODUCT_*` 前缀，实现 `com.qmp.kernel.common.ErrorCode`）
- `dto`：`SkuInfoResponse`（snake_case 字段，对应 OpenAPI 响应示例）

## 多租户调试提示

本地调试需带 `X-Tenant-Id: 1001` 请求头，否则 `inventory-kernel` 的租户拦截器会用默认值 `0`
作为查询条件，查不到种子数据。

## 范围边界

仅负责商品/票种主数据的查询与维护，不涉及库存、价格、订单逻辑——这些分别属于
`inventory-service`/`pricing-service`/`order-service`，按 ADR-010「模块化单体」原则不跨模块访问表。
