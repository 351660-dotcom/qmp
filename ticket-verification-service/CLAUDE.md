# ticket-verification-service

核验中心（门票全链路，对应 docs/07/09/10 文档）。负责出票、核验、申请退票
（`ticket_credential`/`verification_record`），核验成功发布 `TicketVerified`，
消费 `RefundSucceeded` 联动释放库存。订单创建链路中第六个被调用的服务
（`order-service` 消费 `PaymentSucceeded` 时通过 `POST /api/v1/credentials` 出票）。

## 端口与依赖

- HTTP 端口：8086
- 数据库：`ticket_verification_db`（库已在 `docker/mysql/init/01-init-databases.sql` 中创建）
- RocketMQ：`${ROCKETMQ_NAMESRV}`（发布 `TicketVerified`、消费 `RefundSucceeded`）
- 同步依赖：payment-service（退款）、inventory-service（释放预占），地址见
  `verification.client.payment-base-url` / `inventory-base-url`（docker-compose 注入服务名）
- Flyway：`V1__init.sql`（`ticket_credential`/`verification_record`/`processed_event`）。
  无种子数据——凭证由出票动态生成。

## 对外接口（09 文档七）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/credentials` | 出票，幂等键 = `order_item_id`，重复调用返回已生成凭证列表 |
| POST | `/api/v1/credentials/verify` | 核验，入参 `{verify_code, terminal_id}` |
| POST | `/api/v1/credentials/{credential_id}/refund-request` | 申请退票，内部同步调 payment-service 创建退款 |

错误码：`VERIFICATION_CREDENTIAL_NOT_FOUND`(404)、`VERIFICATION_INVALID_SIGNATURE`(400)、
`VERIFICATION_ALREADY_VERIFIED`(409)、`VERIFICATION_EXPIRED`(409)、
`VERIFICATION_NOT_VERIFIABLE`(409)、`VERIFICATION_REFUND_NOT_ALLOWED`(409)。

## 凭证状态机（07 文档 2.4）

`UNUSED` →(核验)→ `VERIFIED`（终态）
`UNUSED` →(申请退票)→ `REFUNDING` →(RefundSucceeded)→ `REFUNDED`（终态）
`UNUSED` →(过期)→ `EXPIRED`（终态，由 `ExpireCredentialJob` 扫描 `sale_date < today` 仍 UNUSED 的凭证置位，no-show 语义：不退款、不释放库存）
`VERIFIED` → `VOIDED`（人工误核销撤销，v1 不实现入口）

## verify_code 设计（07 文档 2.3「签名串，支持离线验签」）

格式：`base64url(payloadJson).hex(HMAC-SHA256(payload, secret))`，payload 内嵌
`{cid, oii, sku, d}`。核验时重算 HMAC 常量时间比对，不符返回 400 `INVALID_SIGNATURE`。
边缘节点持同一 secret 即可离线验签（满足 07 文档 3「核销不依赖订单服务在线」）。

**按租户/景区独立密钥 + 轮换**（`verify_key` 表，V2 迁移）：payload 内嵌 `kid`(=`verify_key.id`)。
出票时取该景区 ACTIVE 密钥签名（`VerifyKeyService.resolveActive`）；验签按码内 kid 取对应密钥
（含已轮换 RETIRED 的，使旧码仍可验）。后台 `POST /admin/v1/verify-keys/rotate?scenic_id=` 轮换：
生成新 ACTIVE 版本、旧版本置 RETIRED。景区未配置密钥时 `kid=0` 回落全局密钥 `VERIFY_CODE_SECRET`（兼容）。

## 发布的事件

| event_type | topic | 触发 | payload |
|---|---|---|---|
| `TicketVerified` | `ticket-verification.ticket-verified` | 核验成功 | `{order_id, order_item_id, credential_id, verified_at}` |

分区 key = `order_id`（`syncSendOrderly`），保证同一订单事件顺序消费（09 文档 1.3）。

## 消费的事件

| event_type | topic | consumer_group | 动作 |
|---|---|---|---|
| `RefundSucceeded` | `payment.refund-succeeded` | `ticket-verification-refund-consumer` | 据 credential_id 定位凭证 → 释放库存预占（reservation_id=order_item_id）→ 凭证置 `REFUNDED` |

消费幂等：`processed_event (consumer_group, event_id)` 去重。处理顺序为
**先释放库存（幂等）→ 再置终态（幂等）→ 最后写 processed_event**，
任一步失败不写去重记录，靠 RocketMQ 重投重跑（ORDERLY 模式，与发布端 order_id 分区一致）。

## v1 扩展决策（作为产品负责人按推荐方案直接执行，供事后 review）

1. **`ticket_credential` 增加 6 个扩展列**：`order_id`/`scenic_id`/`sale_date`/`unit_price`/
   `payment_id`/`refund_policy_snapshot`。*理由*：核验要写 `verification_record.scenic_id`，
   退票要算可退窗口（`sale_date`+`refund_policy_snapshot`）、退款金额（`unit_price`×`refund_ratio`）、
   回调 payment（`payment_id`），发 `TicketVerified` 要 `order_id`。这些值在 order-service 出票时均已掌握，
   冗余下发让本服务自给自足，避免核验/退票时再同步回查他服务（不破坏 ADR-005 模块边界）。
2. **`order_item_id` 用 `VARCHAR(64)` 而非 10 文档 7.1 的 `BIGINT`**。*理由*：09 文档示例
   `OI-20260701-000123` 为字符串，且与 inventory 的 `reservation_id`（VARCHAR）约定一致
   （`reservation_id` = `order_item_id`）。这是对 10 文档的有意偏离。
3. **`IssueCredentialsRequest` 扩展 `order_id`/`scenic_id`/`unit_price`/`payment_id`**
   （超出 09 文档示例字段）：由出票方 order-service 透传。
4. **退款同步链路**：申请退票先把凭证置 `REFUNDING` 并提交，再调 payment-service
   （payment v1 同步完成退款并发 `RefundSucceeded`）；凭证置 `REFUNDED` 与库存释放在
   `RefundSucceeded` 消费侧完成。这样退票申请响应只承诺「已受理」（返回 refund_id），
   终态与库存释放走事件最终一致。
5. **可退窗口规则**：`refund_policy_snapshot.type=NONE` 不可退；否则若有 `cutoff_hours`，
   要求当前时间早于「`sale_date` 0 点 − cutoff_hours」。退款金额 = `unit_price` × `refund_ratio`（默认 1.0）。
6. **库存释放无条件执行**：09 文档八.2 提「在可释放窗口内才释放」，因申请退票已校验过窗口，
   消费侧不再二次判断，直接释放。后续若出现「过期退款不退库存」场景，应在出票时落「可释放截止时间」列再判断。
7. **核验失败的 verification_record**：签名无效（400）时无法可靠定位 scenic_id，故不写流水；
   `ALREADY_VERIFIED`/`EXPIRED`/`NOT_VERIFIABLE` 会写流水（result 记对应值）。

## 多租户调试提示

本地调试需带 `X-Tenant-Id: 1001` 请求头。`verify` 接口虽然 verify_code 内不含 tenant，
但仍按请求头租户做 SQL 行级过滤（终端归属某租户）。消费 `RefundSucceeded` 时按事件 envelope 的
`tenant_id` 设置 `TenantContext` 后再查询。

## 范围边界

仅门票核销凭证与核验流水。不负责订单聚合状态（order-service 消费 `TicketVerified` 自行更新）、
不负责真实退款渠道（payment-service）、不负责库存账本（inventory-service）。
