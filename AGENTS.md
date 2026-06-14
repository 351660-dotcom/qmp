# AGENTS.md

本文件面向在本仓库工作的编码 agent（落地 ADR-010）。完整工程约定见根 [CLAUDE.md](CLAUDE.md)
与各服务目录下的 `CLAUDE.md`；本文件只列「动手前必读」的要点与命令。

## 动手前

1. 先读目标服务的 `CLAUDE.md`（端口/库/接口/事件/**v1 决策**/范围边界），再改代码。
2. 代码与 `docs/` 冲突时，以服务 `CLAUDE.md` 记录的 v1 决策为准；新增偏离要写进该 CLAUDE.md。
3. 不跨模块访问数据库表（ADR-005）；服务间只走 REST/MQ 契约（契约见 `docs/09`）。

## 构建 / 测试

```bash
mvn -B -ntp package                                  # 编译全模块（集成测试默认跳过）
docker compose up -d --build                          # 本地拉起整套依赖 + 服务
RUN_GOLDEN_PATH=true mvn -B -pl integration-tests test  # 跑黄金路径端到端（需整套已就绪）
```

- 单模块构建：`mvn -B -pl <module> -am package`（各服务 Dockerfile 即用此方式，构建上下文为仓库根）。
- 本仓库 Java 21 / Spring Boot 3.3.5 / MyBatis-Plus 3.5.7 / RocketMQ 5.3.0 / MySQL 8.4 / Redis 7。

## 新增一个服务时

- `<parent>` 指向 `com.qmp:scenic-saas-platform`，依赖 `inventory-kernel`（自动带 web/mybatis-plus/
  统一响应/租户拦截）；包名 `com.qmp.{service}`；`finalName=<service>`；在根 `pom.xml` 的 `<modules>` 登记。
- 加 `Dockerfile`（多阶段，`mvn -pl <service> -am -DskipTests package`，构建上下文=仓库根）+
  `application.yml`（端口、`${MYSQL_HOST}`/`${MYSQL_PORT}`、必要时 `${ROCKETMQ_NAMESRV}`）+
  Flyway `V1__init.sql` + `CLAUDE.md`；在 `docker-compose.yml` 注册服务与依赖；
  在 `docker/mysql/init/01-init-databases.sql` 建库。

## 代码风格

- 对外 DTO 字段 snake_case（`@JsonProperty`）；实体用 MyBatis-Plus 注解，`created_at`/`updated_at`
  标 `insertable=false, updatable=false`（DB 默认值维护）。
- 抛 `BizException(XxxErrorCode.YYY)`，勿手写 HTTP 状态/响应体（`GlobalExceptionHandler` 统一处理）。
- 事件发布 `EventEnvelope.of(type, tenantId, payload)` + `syncSendOrderly(topic, event, order_id)`；
  消费实现 `RocketMQListener<String>`，用注入的 `ObjectMapper` 按 `TypeReference<EventEnvelope<X>>` 解析，
  先 `processed_event` 去重、处理、最后写去重。

## 提交前自检

- 是否带 `X-Tenant-Id` 语义（新表是否需要 `tenant_id` + 是否要进 IGNORE_TABLES）。
- 幂等键是否明确（创建类接口都要有业务幂等键，见 `docs/09`）。
- 是否更新了对应服务的 `CLAUDE.md`（接口/事件/决策变更）。
