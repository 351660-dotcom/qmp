# dining-pos-service

餐饮收银 POS（ADR-008/017，对应 docs/12 文档一~四）。桌台/台账/点单/沽清/结账。

## 端口与依赖

- HTTP 端口：8090
- 数据库：`dining_db`
- RocketMQ：结账后发布 `DiningChecked`（供 supply-chain 按 BOM 核减库存）
- 同步依赖：member-service（会员储值抵扣 DeductWallet），`dining.client.member-base-url`
- Flyway：`V1__init.sql`（dining_table / table_order / order_line / dish_availability）

## 对外接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/dining/table-orders` | 开台（IDLE→OCCUPIED，建台账） |
| GET | `/api/v1/dining/table-orders/{id}` | 台账详情（含点单项） |
| POST | `/api/v1/dining/table-orders/{id}/lines` | 加菜（沽清校验） |
| POST | `/api/v1/dining/lines/{lineId}/send-to-kds` | 送厨房 ORDERED→SENT_TO_KDS |
| POST | `/api/v1/dining/lines/{lineId}/advance` | KDS 推进 COOKING/READY/SERVED |
| POST | `/api/v1/dining/lines/{lineId}/cancel` | 退菜（未制作）→CANCELLED |
| POST | `/api/v1/dining/lines/{lineId}/return` | 退菜（已制作）→RETURNED |
| POST | `/api/v1/dining/table-orders/{id}/checkout` | 结账（会员储值抵扣 + 当面收讫） |
| PUT | `/api/v1/dining/dishes/availability` | 设置沽清/恢复 |
| POST/GET | `/admin/v1/tables` | 后台桌台维护 |

错误码 `DINING_*`：TABLE_NOT_FOUND/NOT_IDLE、TABLE_ORDER_NOT_FOUND/NOT_OPEN、
ORDER_LINE_NOT_FOUND、LINE_INVALID_STATE、DISH_SOLD_OUT、CHECKOUT_LINE_NOT_READY。

## 点单项状态机（12 文档 1.5）

`ORDERED →(送厨)→ SENT_TO_KDS → COOKING → READY → SERVED`（终态）；
不过厨房（饮料/零售）加菜即 `SERVED`；`ORDERED/SENT_TO_KDS →取消→ CANCELLED`；
`COOKING/READY/SERVED →退菜→ RETURNED`（不退料，损耗计入）。台账金额 = Σ(非 CANCELLED/RETURNED 行 subtotal)。

## 结账（12 文档四）

校验所有点单项 ∈ {SERVED, CANCELLED, RETURNED}；payable = Σ SERVED subtotal。
若 `use_wallet` 且台账关联 member 且储值余额≥payable：整额走会员储值（member DeductWallet，
幂等键 `DINING:{tableOrderId}`），否则整笔储值抵扣为 0、全额转聚合支付（12 文档 4.2 语义）。
结账置 `CLOSED`、桌台 `CLEANING`，发布 `DiningChecked`。

## 发布事件

| event_type | topic | payload |
|---|---|---|
| `DiningChecked` | `dining.dining-checked` | `{table_order_id, merchant_id, lines:[{sku_id, quantity}]}`（SERVED 行） |

供 supply-chain-service 按 BOM 核减门店库存（12 文档 6.3，异步、不阻塞结账）。

## v1 范围与简化

1. **菜单价**由 POS 端在加菜请求中传入快照（未接价格中心）。
2. **KDS 工单**未单独建 `kitchen_ticket` 表，直接在 `order_line` 状态机上推进（一对多合并工单后续补）。
3. **结账支付**：储值抵扣走真实 member 调用；剩余 `payable_amount` v1 视为当面（CASH/POS）收讫记账，
   未接 payment-service 在线聚合支付的 pay_params/异步回调（可按门票链路后补）。
4. **未实现**：等位排队 QueueTicket、并台/转台、折扣/赠菜/反结账审批工作台（12 文档 4.3）、
   零售无桌台路径（需 merchant_id 入参）、成本核算 CostSnapshot。
5. **退菜审批**：`return` v1 未接审批（12 文档 4.3），直接置 RETURNED。

## 多租户调试

调用带 `X-Tenant-Id` 头；储值抵扣由 MemberClient 透传当前租户。

## 范围边界

仅餐饮前厅/收银。库存/BOM 在 supply-chain-service；会员储值在 member-service；不跨模块访问表（ADR-005）。
