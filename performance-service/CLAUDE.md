# performance-service

演出/游船/游乐（ADR-025，对应 docs/14 文档）。场次库存 + 座位库存 + 手牌二次消费。

## 端口与依赖

- HTTP 端口：8092
- 数据库：`performance_db`
- RocketMQ：消费 `PaymentSucceeded`（确认预订）
- 同步依赖：payment-service（创建支付单）
- Flyway：`V1__init.sql`（performance_session / session_inventory_bucket / seat_inventory_bucket /
  performance_reservation / performance_booking / wristband_account / wristband_ledger / processed_event）

## 对外接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/performance/bookings/session` | 场次预订（游船/游乐，无座位） |
| POST | `/api/v1/performance/bookings/seat` | 选座预订（剧院/舱位） |
| POST | `/api/v1/performance/bookings/{id}/pay` | 发起支付 |
| POST | `/api/v1/performance/bookings/{id}/cancel` | 取消（未支付） |
| GET | `/api/v1/performance/bookings/{id}` | 预订详情 |
| POST | `/api/v1/performance/wristbands` | 办理手牌（可初始充值） |
| GET/POST | `/api/v1/performance/wristbands/{id}[/recharge|/consume]` | 手牌查询/充值/消费 |

后台 `/admin/v1`：建场次、上下架、铺场次库存 `session-buckets`、铺座位库存 `seat-buckets`。

错误码 `PERFORMANCE_*`：SESSION_NOT_FOUND/NOT_ON_SALE、BUCKET_NOT_FOUND、INSUFFICIENT、
BOOKING_NOT_FOUND/INVALID_STATE、WRISTBAND_NOT_FOUND、INSUFFICIENT_WRISTBAND、INVALID_QUOTA/AMOUNT。

## 库存模型（14 文档一/二，ADR-025）

- **场次库存** `session_inventory_bucket`：维度 = session_id（对照门票 sale_date+time_slot_id），无座位业态。
- **座位库存** `seat_inventory_bucket`：维度 = session_id+seat_id，**同一套防超卖机制、粒度更细**
  （每座一条桶，total_quota=座位容量，剧院座位=1，舱位=定员）。
- 两类桶均复用 `inventory-kernel` 的 `InventoryBucketBase` + 四条条件更新 SQL（独立表，不共门票表）。
- 预占状态机复用门票：`HOLDING→CONFIRMED`（支付成功）/`RELEASED`（取消）。`bucket_ref` 区分 SESSION/SEAT。

## 与 payment 的复用

`booking_id` 作 payment 的 order_id；`payment.payment-succeeded` 由门票 order-service、酒店、本服务
各自按 order_id 认领（consumer_group `performance-payment-consumer`），未命中忽略。

## 手牌/腕带二次消费（14 文档四）

`wristband_account` + `wristband_ledger`，办理/充值/消费/查询；幂等键 `(source_ref, type)`，
消费条件更新防负。与会员储值 `DeductWallet`/手牌 `DeductWristbandBalance` 同构（14 文档 4.2），
可供 dining-pos 结账新增「手牌支付」方式调用。

## v1 范围与简化（作为产品负责人按推荐方案执行，供 review）

1. **自包含编排**（类比 hotel）：v1 在本服务内建 `performance_booking` 聚合并自行调 payment。
   14 文档建议「复用 order-service，OrderItem 扩展 session_id/seat_id，order-service 按 bucket_ref 路由」——
   该目标架构需改动 order-service（门票黄金路径），为不影响既有链路，v1 先自包含；后续可迁移到 order 统一编排。
2. **价格**用 session.base_price（票区差异定价/价格中心后续接）。
3. **座位图模板** SeatMap/Seat 未单独建表，座位桶由后台直接按 seat_id 铺（座位图模板后续补）。
4. **安全限制声明**（14 文档 1.3 SafetyDeclarationSnapshot）、**场次取消批量退款**（SessionCancelled）、
   **会员积分打通**（手牌消费→积分）未做。
5. 超时关单（已实现）：`PerformanceExpireBookingJob` 按 `performance.cancel-job` 配置（默认 60s）扫描创建
   已超 `performance.reservation.hold-minutes`（默认 15min）仍 `PENDING_PAYMENT` 的预订，释放场次/座位预占
   + 置 `CANCELLED`（幂等 + 乐观锁）。基于 `created_at` 判定。与门票 order / 酒店预订关单任务同构。

## 多租户调试

调用带 `X-Tenant-Id` 头；消费事件按 envelope 的 `tenant_id` 设置 `TenantContext`。

## 范围边界

仅演出/游船/游乐域。支付/会员各归其服务，不跨模块访问表（ADR-005）。
