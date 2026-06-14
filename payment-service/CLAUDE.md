# payment-service

支付分账中心（门票全链路，对应 docs/07/09/10 文档）。负责支付单/分账/退款
（`payment_order`/`settlement_record`/`refund_record`），是订单创建链路中第五个被调用的服务
（`order-service` 通过 `POST /api/v1/payments` 创建支付单）。

## 端口与数据库

- HTTP 端口：8085
- 数据库：`payment_db`（库已在 `docker/mysql/init/01-init-databases.sql` 中创建）
- RocketMQ：`${ROCKETMQ_NAMESRV}`（docker-compose 中为 `rocketmq-namesrv:9876`）
- Flyway：`V1__init.sql`（表结构，见 10 文档 §8.1-8.3）。本服务无需种子数据——
  `payment_order` 由 `order-service` 创建订单时通过 `order_id` 动态生成。

## 对外接口（09 文档六）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/payments` | 创建支付单，幂等键 = `order_id`（1:1，`uk_order`） |
| POST | `/api/v1/payments/{payment_id}/close` | 关闭支付单（幂等）；已支付时返回 `409 PAYMENT_ALREADY_PAID` |
| POST | `/api/v1/payments/{payment_id}/refunds` | 发起退款，幂等键 = `credential_id`（`uk_credential`） |

错误码：`PAYMENT_NOT_FOUND`(404)、`PAYMENT_ALREADY_PAID`(409)。

## v1 模拟渠道回调（替代 09 文档 6.1）

真实微信/支付宝签名校验留待渠道接入链路。v1 提供内部测试端点：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/internal/callbacks/mock` | 入参 `{payment_id, channel_trade_no}`；将支付单置为 `PAID`，写入 `settlement_record`，发布 `PaymentSucceeded` |

## 发布的事件（09 文档 1.3 / 八.2）

| event_type | topic | 触发时机 | payload |
|---|---|---|---|
| `PaymentSucceeded` | `payment.payment-succeeded` | `/internal/callbacks/mock` 标记 PAID | `{order_id, payment_id, amount, channel, paid_at}` |
| `RefundSucceeded` | `payment.refund-succeeded` | `POST /{payment_id}/refunds` | `{order_id, payment_id, refund_id, credential_id, amount}` |

均使用 `rocketMQTemplate.syncSendOrderly(topic, event, orderId)`，按 `order_id` 作为分区 key
保证同一订单的事件顺序消费（09 文档 1.3）。`event` 为 `com.qmp.kernel.event.EventEnvelope`。

## v1 设计决策（按"推荐方案直接执行"原则，供事后 review）

1. **退款同步完成**：`createRefund` 在本次请求内直接写 `refund_record.status=SUCCEEDED`
   并发布 `RefundSucceeded`，不经过异步渠道回调（09 文档六的响应示例 `status: PENDING`
   是真实异步渠道下的中间态；v1 无真实渠道，故省略该中间态）。
2. **分账金额占位**：08/13 文档未定义平台抽成比例，`settlement_record` 暂记
   `platform_amount=0`、`merchant_amount=全额`、`status=SETTLED`。接入真实分账规则后需重算。
3. **`payment_id` 生成规则**：`PAY-{yyyyMMdd}-{order_id 补零至6位}`，与 09 文档示例
   `PAY-20260701-005001`（对应 `order_id=5001`）一致。

## 多租户调试提示

本地调试需带 `X-Tenant-Id: 1001` 请求头，否则 `inventory-kernel` 的租户拦截器会用默认值 `0`
作为查询条件。`/internal/callbacks/mock` 同样需要带租户头（按支付单所属租户）。

## 范围边界

仅负责支付单/分账/退款记录与事件发布，不消费 MQ 事件（消费方为 `order-service`），
不涉及营销优惠/会员钱包等支付方式——这些属于 `marketing-service`/`member-service`（docs/13）。
