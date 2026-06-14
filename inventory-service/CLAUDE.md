# inventory-service

库存中心（门票全链路，对应 docs/06/07/09/10 文档）。落地 ADR-025「日期/场次型」库存桶的参考实现，
是订单创建链路中第四个被调用的服务（`order-service` 通过
`POST /api/v1/inventory/reservations` 创建预占）。

## 端口与依赖

- HTTP 端口：8084
- 数据库：`inventory_db`（库已在 `docker/mysql/init/01-init-databases.sql` 中创建）
- Redis：用于防超卖第一道防线（`inv:{bucket_id}:remain` key），脚本来自
  `inventory-kernel` 的 `scripts/inventory_reserve.lua` / `inventory_release.lua`
- Flyway：`V1__init.sql`（`inventory_bucket`/`inventory_reservation`，见 10 文档 §5.1/5.2）、
  `V2__seed.sql`（黄金路径示例数据：`sku_id=1001`/`sale_date=2026-07-01`，
  `time_slot_id=1` 与 `2` 两个场次桶，各 `total_quota=1000`）

## 两道防线（06 文档一 / ADR-025）

1. **Redis 预扣**（第一道防线，挡量）：`createReservation` 时执行 `inventory_reserve.lua`
   对 `inv:{bucket_id}:remain` 原子 `DECRBY`；余量不足直接返回 `-1` 快速失败。
   - **冷启动**：首次访问某 bucket 时，用 `SETNX` 按 DB 当前余量
     （`total_quota - sold_count - locked_count`）初始化该 key，避免 Redis 为空导致误判售罄。
2. **DB 条件更新**（第二道防线，最终防超卖）：`InventoryBucketMapper.tryLock/confirmLock/releaseLock/releaseSold`
   对应 10 文档 §5.1 的四条条件 UPDATE，影响行数=0 视为失败/拒绝。
   - 若 DB 条件更新失败（Redis 通过但 DB 拒绝），会将 Redis 预扣的数量补偿回滚。

## 预占状态机（07 文档 1.4）

`HOLDING`（创建预占）→ `CONFIRMED`（确认，支付成功）/ `RELEASED`（释放，取消或退票）/
`EXPIRED`（超时未支付，由 `ExpireReservationJob` 扫描置位）。

## 对外接口（09 文档五）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/inventory/availability?sku_id=&sale_date=&time_slot_id=` | 查询库存余量（C 端展示用，非强一致） |
| POST | `/api/v1/inventory/reservations` | 创建预占，幂等键 = `reservation_id`（约定 = `order_item_id`） |
| POST | `/api/v1/inventory/reservations/{reservation_id}/confirm` | 确认预占 HOLDING->CONFIRMED（幂等） |
| POST | `/api/v1/inventory/reservations/{reservation_id}/release` | 释放预占 HOLDING\|CONFIRMED->RELEASED（幂等） |

错误码：`INVENTORY_BUCKET_NOT_FOUND`(404)、`INVENTORY_INSUFFICIENT_STOCK`(409)、
`INVENTORY_RESERVATION_NOT_FOUND`(404)、`INVENTORY_RESERVATION_INVALID_STATE`(409)。

## ExpireReservation 定时任务

`ExpireReservationJob` 每 `inventory.expire-job.fixed-delay-ms`（默认 60s）扫描一次
`status=HOLDING AND hold_expire_at < now()` 的预占，释放 DB 锁定量 + 回补 Redis 余量，置为 `EXPIRED`。

**v1 已知限制**：`inventory_reservation` 表启用了租户行级拦截（ADR-021），单次查询只能扫描一个租户。
本任务按 `inventory.expire-job.tenant-ids`（默认 `1001`，门票黄金路径示例租户）逐一扫描。
租户数量增长后应改为维护租户注册表 + 按租户分片调度，而非在配置中枚举租户 ID。

## 多租户调试提示

本地调试需带 `X-Tenant-Id: 1001` 请求头，否则 `inventory-kernel` 的租户拦截器会用默认值 `0`
作为查询条件，查不到种子数据。

## 范围边界

仅负责门票域 `inventory_bucket`/`inventory_reservation`。酒店 `room_inventory_bucket`、
会议室 `meeting_room_inventory_bucket` 等其他业务域库存按 ADR-025「不共享表，只共享设计模式」
各自建表 + 复用 `inventory-kernel` 的 `InventoryBucketBase`/`InventoryReservationBase`/Lua 脚本，
不在本服务实现。
