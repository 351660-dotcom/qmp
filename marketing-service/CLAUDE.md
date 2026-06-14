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

## v1 范围与简化

1. 仅优惠券 CRUD/发放/核销，**不含**营销规则引擎（PromotionRule/满减折扣买赠返现）、
   秒杀/拼团/砍价、跨商户成本分摊（CostAllocationRule）——均为 13 文档后续迭代。
2. 核销由调用方（order-service 创建订单/PAID 后）显式调用 `redeem`；v1 未做事件驱动自动核销。
3. `revert` 仅改 `status=UNUSED`；`order_id`/`used_at` 受 MyBatis-Plus NOT_NULL 更新策略限制不会被清空
   （status 为权威字段），后续如需清空可加显式 update。

## 多租户调试

调用带 `X-Tenant-Id` 头；模板/实例均带 `tenant_id`，由租户拦截器自动隔离。

## 范围边界

仅营销域。会员积分/储值在 member-service；订单/支付/库存各归其服务，经 REST/MQ 契约交互。
