# order-service

订单编排中心（C 端入口，对应 docs/07/09/10 文档第八节）。是门票全链路的「编排者」：
创建订单时同步编排 product/pricing/member/inventory，支付成功后异步编排
inventory（确认预占）/ticket-verification（出票）。

## 端口与依赖

- HTTP 端口：8087
- 数据库：`order_db`（库已在 `docker/mysql/init/01-init-databases.sql` 中创建）
- RocketMQ：消费 `PaymentSucceeded`、`TicketVerified`（本服务不发布事件）
- 同步依赖：product(8081)/pricing(8082)/member(8083)/inventory(8084)/payment(8085)/
  ticket-verification(8086)，地址见 `order.client.*-base-url`（docker-compose 注入服务名）
- Flyway：`V1__init.sql`（`trade_order`/`order_item`/`processed_event`）。无种子数据——订单动态创建。

## 对外接口（09 文档八）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/orders` | 创建订单，编排校验 + 预占 + 显式补偿 |
| POST | `/api/v1/orders/{order_id}/pay` | 发起支付，透传 payment-service 的 pay_params |
| GET | `/api/v1/orders/{order_id}` | 查询订单详情（含各明细核销聚合） |

错误码：`ORDER_NOT_FOUND`(404)、`ORDER_INVALID_STATE`(409)、
`PRODUCT_SKU_NOT_ON_SALE`(409)、`INVENTORY_INSUFFICIENT_STOCK`(409)、`ORDER_UPSTREAM_ERROR`(502)。

## 创建订单编排（09 文档 8.1，伪分布式事务 + 显式补偿）

```
member.GetMemberStatus → 逐 item: product.GetSku（校验 ON_SALE）→ pricing.GetPrice
  → inventory.CreateReservation(order_item_id)；失败则补偿 release 已预占的前序 item
→ 持久化 trade_order(PENDING_PAYMENT) + order_item（同一本地事务）
```

多 item 预占不在同一本地事务，靠**显式补偿**保证一致性（ADR-001 已选型 Seata，v1 先用补偿跑通，
避免过早引入分布式事务复杂度）。补偿失败仅记日志，留待对账/人工介入。

## 异步事件消费（09 文档 8.2）

| 事件 | topic | consumer_group | 动作 |
|---|---|---|---|
| `PaymentSucceeded` | `payment.payment-succeeded` | `order-service-payment-consumer` | 逐 item `inventory.ConfirmReservation` + `ticket-verification.IssueCredentials`，订单置 `PAID` |
| `TicketVerified` | `ticket-verification.ticket-verified` | `order-service-ticket-verified-consumer` | 对应明细 `verified_count++`；全部核销则订单置 `CLOSED` |

幂等：`processed_event (consumer_group, event_id)` 去重，ORDERLY 消费（与发布端 order_id 分区一致）。
顺序「业务（幂等）→ 写去重」，失败靠 MQ 重投重跑。

## v1 设计决策（作为产品负责人按推荐方案直接执行，供事后 review）

1. **`order_item_id` 用 `VARCHAR(64)`（`OI-{snowflake}`）而非 10 文档 6.2 的 BIGINT**：
   与 inventory.reservation_id / ticket_credential.order_item_id 字符串约定一致（三者必须同型）。
   格式为 `OI-` + 雪花 ID（非 09 示例的 `OI-20260701-000123` 顺序号，避免维护跨天序列），仅形态相近。
2. **`order_item.verified_count`（新增列）**：order-service 不落库单张凭证（凭证归
   ticket-verification-service，ADR-005），用每明细已核销计数表达核销进度。
   订单详情据此返回 `quantity`/`verified_count` 聚合，而非 09 示例的逐张 `credentials[]`。
   后续如需逐张明细，应由 ticket-verification 增「按 order 查凭证」接口，order 透传，而非自存凭证。
3. **退改规则快照占位**：v1 无独立退改策略服务，创建订单时统一写默认
   `{type:TIERED, cutoff_hours:24, refund_ratio:0.8}`（与 09 文档示例一致）到 `order_item.refund_policy_snapshot`，
   出票时透传给凭证。接入商品/营销退改配置后改为按 sku 取真实快照。
4. **单商户购物车**：v1 以 item 的 scenic_id/merchant_id 落 `trade_order`（黄金路径同景区同商户）；
   跨商户购物车需拆分子订单 + 多支付单（08/10 文档已注明 payment 1:1 约束待放开）。
5. **不消费 `RefundSucceeded`**：库存释放由 ticket-verification-service 消费 `RefundSucceeded` 完成
   （它持有 credential→order_item 映射，order-service 无 credential_id 映射）。order-service v1 暂不据退款
   更新 `trade_order.refund_amount`/状态——待退款链路细化时补 order 侧 refund 消费。
6. **`pay` 仅 `PENDING_PAYMENT` 可发起**：payment.createPayment 幂等（键 order_id），重复 pay 返回同一支付单。

## 多租户调试提示

本地调试需带 `X-Tenant-Id: 1001` 请求头；所有下游同步调用由各 client 自动透传当前
`TenantContext` 的租户头。消费事件时按 envelope 的 `tenant_id` 设置 `TenantContext` 后再处理。

## 范围边界

仅订单编排与订单/明细主数据。不持有库存账本/凭证/支付单/价格/会员——分别归
inventory/ticket-verification/payment/pricing/member 服务，按 ADR-005 不跨模块访问表，仅经 REST/MQ 契约交互。
