# member-service

会员中心（门票全链路，对应 docs/07/09/10 文档）。负责会员身份 `member_account` 的查询，
是订单创建链路中第三个被调用的服务（`order-service` 通过
`GET /api/v1/members/{user_id}/status` 判断下单用户是否享受会员价）。

## 端口与数据库

- HTTP 端口：8083
- 数据库：`member_db`（库已在 `docker/mysql/init/01-init-databases.sql` 中创建）
- Flyway：`V1__init.sql`（表结构，见 docs/10 §4.1）、`V2__seed.sql`（黄金路径示例数据：
  `user_id=123` 为会员，与 docs/09 文档示例 `is_member: true` 一致）

## 对外接口（docs/09 文档四）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/members/{user_id}/status` | 查询会员身份 |

- **`user_id` 不存在会员档案时返回 `is_member: false`，不报 404**（视为非会员，见 09 文档四）。

返回格式为 `com.qmp.kernel.common.ApiResponse`（`{code, message, data, trace_id}`）。

## 目录结构

- `entity`：`MemberAccount`（MyBatis-Plus `@TableName`，主键 `user_id` 与 C 端用户账号一致，非雪花 ID）
- `mapper`：`MemberAccountMapper`
- `service`：`MemberQueryService`（查不到记录时返回 `is_member=false` 而非抛异常）
- `controller`：`MemberController`
- `dto`：`MemberStatusResponse`（snake_case 字段，对应 OpenAPI 响应示例）

## 多租户调试提示

本地调试需带 `X-Tenant-Id: 1001` 请求头，否则 `inventory-kernel` 的租户拦截器会用默认值 `0`
作为查询条件，查不到种子数据。

## 会员体系扩展（13 文档一，V3/V4 迁移）

已落地等级 / 积分 / 储值：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/members/{userId}/points` | 查积分余额 |
| POST | `/api/v1/members/{userId}/points/redeem` | 积分抵扣（条件更新防负） |
| GET | `/api/v1/members/{userId}/wallet` | 查储值余额 |
| POST | `/api/v1/members/{userId}/wallet/recharge` | 充值 |
| POST | `/api/v1/members/{userId}/wallet/deduct` | 消费扣减（= 12 文档 DeductWallet） |
| POST/GET | `/admin/v1/levels` | 后台维护会员等级 |

- **积分入账**：消费 `OrderPaid`（topic `order.order-paid`，order-service 发布）→ 按金额发放积分
  （v1 规则 1 元=1 积分，向下取整），consumer_group `member-service-order-paid-consumer`，ORDERLY。
- **幂等**：积分/储值账本 `(source_ref, type)` 唯一约束 + 事务回滚；OrderPaid 叠加 `processed_event` 去重。
  积分来源 `source_ref = "ORDER:{order_id}"`。
- **余额安全**：`PointAccountMapper.deductBalance`/`MemberWalletMapper.deductBalance` 用
  `WHERE balance >= ?` 条件更新（与 ADR-018 库存防超卖同原则）。
- **等级升级**：`earn` 时累加 `member_account.growth_value` 并按 `member_level.min_growth_value` 阈值自动定级。
- 错误码：`MEMBER_INSUFFICIENT_POINTS`(409)、`MEMBER_INSUFFICIENT_BALANCE`(409)、`MEMBER_INVALID_AMOUNT`(400)。

### v1 简化
- 储值充值 `recharge` 为直接入账；真实充值资金应走 payment 的「储值专户」+ 消费时再分账（13 文档 1.4），后续补。
- 积分过期（EXPIRE）、等级降级/有效期、跨商户积分分摊（13 文档二）未做。

## 范围边界

会员身份/等级/积分/储值归本服务；优惠券/营销规则在 marketing-service；不跨模块访问表（ADR-005）。
