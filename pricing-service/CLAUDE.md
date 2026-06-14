# pricing-service

价格中心（门票全链路，对应 docs/07/09/10 文档）。负责价格日历 `price_calendar` 的查询，
是订单创建链路中第二个被调用的服务（`order-service` 通过 `GET /api/v1/price` 取价快照写入
`order_item.unit_price`）。

## 端口与数据库

- HTTP 端口：8082
- 数据库：`pricing_db`（库已在 `docker/mysql/init/01-init-databases.sql` 中创建）
- Flyway：`V1__init.sql`（表结构，见 docs/10 §3.1）、`V2__seed.sql`（黄金路径示例数据：
  `sku_id=1001`/`sale_date=2026-07-01`，`RETAIL=98.00`/`MEMBER=88.00`，与 docs/09 文档示例一致）

## 对外接口（docs/09 文档三）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/price?sku_id=&sale_date=&is_member=` | 查询指定票种/日期/会员类型的价格 |

- 找不到当日价格时返回 `404 PRICING_NOT_CONFIGURED`——**订单创建应失败而非默认 0**（见 09 文档三）。

返回格式为 `com.qmp.kernel.common.ApiResponse`（`{code, message, data, trace_id}`）。

## 目录结构

- `entity`：`PriceCalendar`（MyBatis-Plus `@TableName`，主键为雪花 ID `@TableId(ASSIGN_ID)`）
- `mapper`：`PriceCalendarMapper`
- `service`：`PriceQueryService`（按 `sku_id + sale_date + price_type` 唯一索引查询）
- `controller`：`PriceController`
- `error`：`PricingErrorCode`（`PRICING_*` 前缀，实现 `com.qmp.kernel.common.ErrorCode`）
- `dto`：`PriceResponse`（snake_case 字段，对应 OpenAPI 响应示例）

## 多租户调试提示

本地调试需带 `X-Tenant-Id: 1001` 请求头，否则 `inventory-kernel` 的租户拦截器会用默认值 `0`
作为查询条件，查不到种子数据。

## 范围边界

仅负责门市价/会员价的查询与维护，不涉及营销优惠券/秒杀等动态价格计算——这些属于
`marketing-service`（见 docs/13），按 ADR-010「模块化单体」原则不跨模块访问表。
