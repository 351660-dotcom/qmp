# 景区文旅 SaaS 平台 · 门票全链路（CLAUDE.md）

多租户景区文旅 SaaS。当前阶段落地 **ADR-005 第一条联调链路：门票全链路**，按 ADR-010
「AI-native 工程化」组织为**模块化单体**——每个服务是独立 Maven 模块 + 独立可运行 Spring Boot 应用 +
独立 DB schema（同一 MySQL 实例），服务间只经 **REST + RocketMQ 契约**交互，**不跨模块访问表**。

## 模块总览

| 模块 | 端口 | DB schema | 职责 | 关键事件 |
|---|---|---|---|---|
| `inventory-kernel` | - (jar) | - | 共享内核：统一响应/错误码、EventEnvelope、租户上下文+拦截器、库存桶基类+Lua | - |
| `product-service` | 8081 | product_db | 门票/票种主数据查询 | - |
| `pricing-service` | 8082 | pricing_db | 价格日历查询（门市价/会员价） | - |
| `member-service` | 8083 | member_db | 会员身份查询 | - |
| `inventory-service` | 8084 | inventory_db | 库存预占/确认/释放（两道防线） | - |
| `payment-service` | 8085 | payment_db | 支付单/分账/退款 | 发 `PaymentSucceeded`/`RefundSucceeded` |
| `ticket-verification-service` | 8086 | ticket_verification_db | 出票/核验/退票申请 | 发 `TicketVerified`，消费 `RefundSucceeded` |
| `order-service` | 8087 | order_db | 编排中心（C 端入口） | 消费 `PaymentSucceeded`/`TicketVerified`，发 `OrderPaid` |
| `hotel-pms-service` | 8088 | hotel_db | 酒店 PMS（房型/房晚库存/连住预订） | 消费 `PaymentSucceeded` |
| `marketing-service` | 8089 | marketing_db | 营销中心（v1 优惠券） | - |
| `dining-pos-service` | 8090 | dining_db | 餐饮收银 POS（桌台/台账/点单/沽清/结账） | 发 `DiningChecked` |
| `supply-chain-service` | 8091 | supply_chain_db | 供应链（库存/BOM 自动核减） | 消费 `DiningChecked` |
| `performance-service` | 8092 | performance_db | 演出/游船/游乐（场次/座位库存 + 手牌） | 消费 `PaymentSucceeded`，发 `WristbandConsumed` |
| `reconciliation-service` | 8093 | reconciliation_db | 跨业态统一对账（资金归集 + 日汇总） | 消费 `PaymentSucceeded`/`RefundSucceeded`/`WalletConsumed`/`WristbandConsumed` |
| `integration-tests` | - | - | 各业态黄金路径端到端测试 | - |

> member-service 储值消费发 `WalletConsumed`；reconciliation-service 汇集全业态资金口径
> （门票/酒店/演出经 payment，餐饮经会员储值，水乐园经手牌）做统一对账。

> member-service 已扩展会员等级/积分/储值（13 文档），消费 `OrderPaid` 发放积分。
> 跨业态事件主题 `payment.payment-succeeded` 由门票 order-service 与酒店 hotel-pms-service 各自按 order_id 认领。

每个服务目录下有自己的 `CLAUDE.md`，记录其端口/库/接口/事件/**v1 设计决策**与范围边界——
改某服务前先读它的 `CLAUDE.md`。

## 约定（全平台统一，新服务必须沿用）

- **包名**：`com.qmp.{service}`（如 `com.qmp.order`）；内核 `com.qmp.kernel`。
- **groupId** `com.qmp`，**version** `0.1.0-SNAPSHOT`，各服务 `<parent>` 指向根 `scenic-saas-platform`。
- **HTTP 响应**：统一用 `com.qmp.kernel.common.ApiResponse`（`{code,message,data,trace_id}`），
  字段对外 snake_case（DTO 上 `@JsonProperty`）。错误经 `BizException(ErrorCode)` + `GlobalExceptionHandler`。
- **错误码**：各服务 `error` 包内枚举实现 `ErrorCode`，命名 `{SERVICE}_{ERROR_NAME}`。
- **多租户**：所有业务表带 `tenant_id`，由 `TenantLineHandlerImpl` 自动注入 SQL 条件；
  调用务必带 `X-Tenant-Id` 头（默认 `0`）。`processed_event` 表除外（已在 IGNORE_TABLES）。
- **主键**：应用层雪花 ID（`@TableId(ASSIGN_ID)`）；字符串业务键除外
  （`payment_id`、`order_item_id`/`reservation_id`、`credential` 的 `order_item_id`）。
- **MQ**：`EventEnvelope`（`event_id`/`event_type`/`schema_version`/`tenant_id`/`occurred_at`/`payload`）；
  topic 命名 `{domain}.{event-name-kebab}`；分区 key 取 `order_id`（`syncSendOrderly`）顺序消费；
  消费方 `processed_event (consumer_group, event_id)` 去重，**先处理（幂等）后写去重**。
- **DB**：金额 `DECIMAL(10,2)`；状态用 `VARCHAR` 非 `ENUM`；`order`→`trade_order`；Flyway 迁移在
  各服务 `src/main/resources/db/migration`。
- **服务间调用**：用 `RestTemplate` client，自动透传 `X-Tenant-Id`（取当前 `TenantContext`）。

> **重要约定**：`order_item_id` = inventory 的 `reservation_id` = ticket_credential 的 `order_item_id`，
> 三者必须同型。v1 统一用 `VARCHAR(64)`（形如 `OI-{雪花}`），是对 10 文档（BIGINT）的有意偏离，
> 详见 order-service / inventory-service / ticket-verification-service 的 CLAUDE.md。

## 本地运行

```bash
docker compose up -d --build      # 拉起 MySQL/Redis/RocketMQ + 7 个服务
# 调试示例（务必带租户头）
curl -H 'X-Tenant-Id: 1001' http://localhost:8081/api/v1/skus/1001
```

数据库初始化见 `docker/mysql/init/01-init-databases.sql`，各服务 Flyway 自动建表 + 种子数据
（黄金路径示例：tenant 1001 / scenic 3001 / merchant 2001 / sku 1001 / sale_date 2026-07-01）。

## 后台管理（Admin API）

主数据可由后台维护（替代种子 SQL），统一前缀 `/admin/v1`，需带 `X-Tenant-Id` 头：
product（商品/票种/上下架）、pricing（价格 upsert）、inventory（库存桶 upsert）、order（订单列表）。
完整清单与造数示例见 [docs/后台管理接口.md](docs/后台管理接口.md)。
**鉴权**：`inventory-kernel` 的 `AdminAuthFilter` 统一保护 `/admin/**`——配置 `ADMIN_API_TOKEN` 后
须带 `X-Admin-Token` 头（否则 401），未配置则放行（本地/单测友好）。docker-compose 已注入
`ADMIN_API_TOKEN=scenic-admin-dev-token`。细粒度 RBAC（ADR-011）后续补。

## 测试

- 端到端黄金路径：`integration-tests` 模块 `TicketGoldenPathIT`（默认跳过，
  `RUN_GOLDEN_PATH=true` 且整套服务已就绪时执行）。
- CI：`.github/workflows/ci.yml` 两个 job——`build`（`mvn package` 编译全模块）、
  `golden-path`（`docker compose up --build` + 就绪等待 + 跑 IT）。

## 文档

设计依据在 `docs/`：02（ADR）、06（库存防超卖）、07/08（门票域模型）、09（服务契约/OpenAPI+事件）、
10（数据库设计）。代码与文档冲突时，**以各服务 CLAUDE.md 记录的 v1 决策为准**（已说明对文档的偏离与理由）。

## 范围

当前仅门票全链路（07-10 文档）。酒店 PMS（11）、餐饮零售（12）、会员营销（13）、演出游船游乐（14）
为后续链路，按 ADR-025「不共享表，只共享设计模式」各自建模 + 复用 `inventory-kernel`。
