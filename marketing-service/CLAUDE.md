# marketing-service

营销中心（对应 docs/13 文档）。v1 聚焦**优惠券中心**：模板管理 + 发券/查券/核销/回退。

## 端口与数据库

- HTTP 端口：8089
- 数据库：`marketing_db`（已在 `docker/mysql/init/01-init-databases.sql` 建库）
- Flyway：`V1__init.sql`（coupon_template / coupon_instance）

## 对外接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/coupons/issue` | 发券（校验 issue_quota），返回券实例 |
| GET | `/api/v1/coupons?user_id=&status=` | 查询用户券 |
| POST | `/api/v1/coupons/{couponId}/redeem` | 核销 UNUSED→USED，关联 order_id（订单 PAID 后调用） |
| POST | `/api/v1/coupons/{couponId}/revert` | 回退 USED→UNUSED（订单取消/退款，幂等） |

后台（`/admin/v1`）：`POST /coupon-templates`、`GET /coupon-templates`。

错误码 `MARKETING_*`：TEMPLATE_NOT_FOUND、COUPON_NOT_FOUND、COUPON_NOT_USABLE、ISSUE_QUOTA_EXCEEDED。

## 营销规则引擎（13 文档三，V2 迁移）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/promotions/calculate` | 试算优惠：`{user_id, items:[{sku_id,quantity,unit_price}]}` → `{original_amount, discount_amount, payable_amount, applied_rules[]}` |
| POST/GET | `/admin/v1/promotion-rules` | 维护规则（DISCOUNT:{discount_rate} / FULL_REDUCTION:{threshold,reduce}） |

计算顺序（13 文档 3.3）：① DISCOUNT 乘法（取力度最大一条）→ ② FULL_REDUCTION 加法（取满门槛且减额最大一条）。
v1 叠加策略简化为「每类取最优」（EXCLUSIVE）；下单时由调用方快照 `applied_rules` 留痕。

## 秒杀（13 文档 5.1，V2 迁移）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/v1/seckill-activities` | 建活动（sku/秒杀价/时间窗/状态） |
| POST | `/admin/v1/seckill-buckets` | 铺秒杀库存（按 activity_id 幂等） |
| POST | `/api/v1/seckill/{activityId}/snap` | 抢购：校验活动进行中 + 限购1 + 独立桶 tryLock |
| POST | `/api/v1/seckill/reservations/{reservationId}/release` | 放弃/释放 |

- **独立库存桶** `seckill_inventory_bucket`（ADR-025，与常规库存彼此独立，复用 inventory-kernel 防超卖条件更新）。
- **限购 1**：`seckill_reservation.reservation_id = activity_id:user_id`（唯一即限购）。
- 错误码：`MARKETING_SECKILL_NOT_ACTIVE`/`SECKILL_SOLD_OUT`/`SECKILL_ALREADY_SNAPPED` 等。

## v1 范围与简化

1. 核销由调用方（order-service 创建订单/PAID 后）显式调用 `redeem`；未做事件驱动自动核销。
2. `revert` 仅改 `status=UNUSED`；`order_id`/`used_at` 受 MyBatis-Plus NOT_NULL 更新策略限制不会被清空。
3. 规则引擎叠加策略简化（每类取最优）；conditions（商品/会员等级/时段范围匹配）v1 未实现。
4. 秒杀 snap 仅预占，未串支付/确认（与订单链路打通后补）；从常规桶 Transfer 切量、活动结束退回常规桶未实现。
5. **未实现**：拼团/砍价（13 文档 5.2/5.3）、跨商户成本分摊 CostAllocationRule（13 文档二）、买赠/返现。

## 多租户调试

调用带 `X-Tenant-Id` 头；模板/实例均带 `tenant_id`，由租户拦截器自动隔离。

## 范围边界

仅营销域。会员积分/储值在 member-service；订单/支付/库存各归其服务，经 REST/MQ 契约交互。
