# reconciliation-service

跨业态统一对账（④）。消费各业态资金事件，汇成统一对账流水 `recon_transaction`，提供按日/商户的对账汇总。

## 端口与依赖

- HTTP 端口：8093
- 数据库：`reconciliation_db`
- RocketMQ：消费 4 个资金事件主题（见下）
- Flyway：`V1__init.sql`（recon_transaction / processed_event）

## 消费的资金事件（覆盖全部业态资金口径）

| event_type | topic | 来源业态 | 记账 |
|---|---|---|---|
| `PaymentSucceeded` | `payment.payment-succeeded` | 门票/酒店/演出（均经 payment） | IN / PAYMENT |
| `RefundSucceeded` | `payment.refund-succeeded` | 退款 | OUT / REFUND |
| `WalletConsumed` | `member.wallet-consumed` | 餐饮等会员储值消费 | IN / WALLET |
| `WristbandConsumed` | `performance.wristband-consumed` | 水乐园手牌二次消费 | IN / WRISTBAND |

- 每个 consumer 独立 group + `processed_event` 去重，ORDERLY。
- 为支撑按商户对账，payment 的 `PaymentSucceeded`/`RefundSucceeded` 已补 `merchant_id`；
  `WalletConsumed`/`WristbandConsumed` 由 member/performance 在消费时发布（带 merchant_id）。

## 对外接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/reconciliation/daily?date=&merchant_id=` | 日对账汇总：in_total/out_total/net/count/by_source |
| GET | `/api/v1/reconciliation/transactions?date=&merchant_id=` | 当日对账流水明细（最多 500） |

## v1 范围与简化

1. 对账维度 = 租户 + 商户 + 日期 + 资金来源；**业务线维度**未单独标注（payment 未带业态标签，
   后续可在创建支付时加 `biz_type` 透传到事件再入对账）。
2. 仅「资金流水归集 + 汇总」；与渠道账单/银行流水的**双边核对（差异勾兑）**、分账 SettlementRecord 的
   勾稽、跨商户成本分摊调整项（13 文档二）入账，均为后续迭代。
3. `recon_date` 取事件发生时刻当日；跨时区/跨日切边界按服务器时区，未做营业日切配置。

## 多租户调试

消费事件按 envelope 的 `tenant_id` 设置 `TenantContext`；查询接口带 `X-Tenant-Id` 头。

## 范围边界

只做对账归集与汇总，不产生资金、不改分账（分账在 payment-service）。不跨模块访问表，只消费事件（ADR-005）。
