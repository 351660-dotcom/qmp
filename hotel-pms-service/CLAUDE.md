# hotel-pms-service

酒店 PMS（ADR-007/020/025，对应 docs/11 文档）。门票链路之后的**第二条业态链路**，
重点示范 ADR-025「不共享表，只共享设计模式」：房晚库存在本服务内独立建表，复用
`inventory-kernel` 的库存桶基类与预占状态机，不共用门票域的表与 inventory-service API。
在酒店域内扮演类似门票 order-service 的「编排者」。

## 端口与依赖

- HTTP 端口：8088
- 数据库：`hotel_db`（已在 `docker/mysql/init/01-init-databases.sql` 建库）
- RocketMQ：消费 `PaymentSucceeded`（确认预订）
- 同步依赖：payment-service（创建支付单），`hotel.client.payment-base-url`
- Flyway：`V1__init.sql`（room_type / room_inventory_bucket / room_night_reservation / room_reservation / processed_event）

## 对外接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/hotel/availability?sku_id=&check_in_date=&check_out_date=` | 逐晚余量 + 整段最小余量（可订上限） |
| POST | `/api/v1/hotel/reservations` | 创建预订（连住多夜原子预占 + 补偿） |
| POST | `/api/v1/hotel/reservations/{id}/pay` | 发起支付（透传 payment 的 pay_params） |
| POST | `/api/v1/hotel/reservations/{id}/cancel` | 取消（v1 仅未支付可取消，释放预占） |
| GET | `/api/v1/hotel/reservations/{id}` | 查询预订单 |

后台（`/admin/v1`）：`POST /room-types`、`PATCH /room-types/{id}/status`、`POST /room-buckets`（按区间铺房晚库存）。

错误码 `HOTEL_*`：ROOM_TYPE_NOT_FOUND/NOT_ON_SALE、BUCKET_NOT_FOUND、INSUFFICIENT_ROOM、
RESERVATION_NOT_FOUND/INVALID_STATE、INVALID_DATE_RANGE、INVALID_QUOTA。

## 核心难点：连住「多夜原子预占」（11 文档 1.3）

一次连住 = 区间 `[check_in, check_out)` 内 N 个不同 `room_inventory_bucket`（同房型、不同 `sale_date`）的预占，
**必须全部成功**；任一晚不足则对已成功的前序晚补偿释放（与门票 order-service 创建订单的显式补偿同构）。
钟点房 `check_in == check_out` 占用「当晚」一个桶。

预占状态机复用门票：`HOLDING → CONFIRMED`（支付成功）/ `RELEASED`（取消）。

## 防超卖

v1 仅用 **DB 条件更新第二道防线**（`RoomInventoryBucketMapper.tryLock/confirmLock/releaseLock/releaseSold`，
SQL 范式同门票 inventory_bucket）。高并发时可叠加 inventory-kernel 的 Redis Lua 第一道防线
（与 inventory-service 完全同款），v1 为控制面积暂未引入。

## 与 payment-service 的复用（跨业态共用支付主题）

预订单 `reservation_id`（雪花数字）直接作为 payment 的 `order_id`，payment 不感知业态差异。
`payment.payment-succeeded` 主题被门票 order-service 与本服务**同时订阅**（不同 consumer_group：
`hotel-pms-payment-consumer`），各自按 `order_id` 是否命中本域订单认领，未命中忽略。
消费幂等：`processed_event (consumer_group, event_id)`。

## v1 范围与简化（作为产品负责人按推荐方案执行，供 review）

1. **价格**：房型上挂 `base_price`，金额 = base_price × 间夜数 × 间数。未接价格日历/连住价/会员价
   （门票 pricing-service 的 price_calendar 以 sku_id+sale_date 维度，后续可直接复用为房晚价）。
2. **退款取消**：v1 仅未支付预订可取消（释放预占）；已支付的退款取消、取消政策（阶梯扣费）留待退款链路细化。
3. **未实现（11 文档其余章节）**：排房 RoomUnit/房态机、客房部 Housekeeping、前台账务 Folio/入住/夜审/退房、
   协议单位、GuestProfile 单一客史、公安治安系统对接、团队订单。本期聚焦「房型 + 房晚库存 + 连住预订 + 支付确认」最小闭环。
4. **超时关单**（已实现）：`HotelExpireReservationJob` 按 `hotel.cancel-job` 配置（默认 60s）扫描创建已超
   `hotel.reservation.hold-minutes`（默认 30 分钟）仍 `PENDING_PAYMENT` 的预订，复用 `cancel()` 的释放逻辑
   （`releaseStay` + 置 `CANCELLED`，幂等 + 乐观锁）。基于 `created_at` 判定，无需额外列。与门票 order 的
   `CancelExpiredOrderJob` 同构。

## 多租户调试

所有调用带 `X-Tenant-Id` 头；消费事件按 envelope 的 `tenant_id` 设置 `TenantContext`。

## 范围边界

仅酒店域。门票/支付/会员等仍归各自服务，经 REST/MQ 契约交互，不跨模块访问表（ADR-005）。
