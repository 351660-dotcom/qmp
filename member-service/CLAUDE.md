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

## 范围边界

本期仅包含会员身份（`is_member`）查询，不包含会员等级/积分/钱包等能力——这些属于
docs/13 设计的 `member-service` 扩展范围（`MemberLevel`/`PointAccount`/`MemberWallet` 等），
将在门票黄金路径之后按需补齐，遵循 ADR-010「模块化单体」原则不跨模块访问表。
