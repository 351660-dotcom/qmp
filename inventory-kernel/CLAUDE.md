# inventory-kernel

门票全链路共享内核（纯 jar 库，无 Spring Boot 启动类）。被其余 7 个服务依赖，提供跨服务复用的横切代码，
依赖此模块即自动生效（通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
注册 `KernelAutoConfiguration`），无需在各服务手写配置。

## 提供的能力

| 包 | 内容 | 对应文档 |
|---|---|---|
| `com.qmp.kernel.common` | `ApiResponse<T>` 统一响应格式、`ErrorCode` 接口、`CommonErrorCode`、`BizException` | 09 文档 1.1 |
| `com.qmp.kernel.web` | `GlobalExceptionHandler`：把 `BizException`/参数校验异常转换为统一响应 + HTTP 状态码 | 09 文档 1.1 |
| `com.qmp.kernel.event` | `EventEnvelope<T>`：MQ 事件统一 envelope（event_id/event_type/schema_version/tenant_id/occurred_at/payload） | 09 文档 1.3 |
| `com.qmp.kernel.context` | `TenantContext`/`TraceContext`（ThreadLocal）+ `RequestContextFilter`（解析 `X-Tenant-Id`/`X-Trace-Id` 请求头） | 09 文档 1.1 / 10 文档通用约定 2 |
| `com.qmp.kernel.mybatis` | `TenantLineHandlerImpl` + `MybatisPlusConfig`：SQL 层自动注入 `tenant_id` 条件 + 分页插件 | 10 文档通用约定 2 / ADR-021 |
| `com.qmp.kernel.inventory` | `InventoryBucketBase`/`InventoryReservationBase`/`ReservationStatus`：「日期/场次型」库存桶 + 预占状态机的参考实现基类；`scripts/inventory_reserve.lua`/`inventory_release.lua`：防超卖第一道防线 Lua 脚本 | ADR-025 / 06 文档 / 07 文档 1.1-1.4 |
| （pom 依赖）`spring-boot-starter-actuator` | 依赖内核即自动暴露 `/actuator/health`（默认仅 health 端点、不展开细节）：供 k8s 探针 / docker-compose healthcheck / 监控使用。`AdminAuthFilter` 只保护 `/admin/**`，不拦 actuator | ADR-004 非功能性需求（可观测性） |

## 各业务域实体如何复用 `InventoryBucketBase` / `InventoryReservationBase`

按 ADR-025：**不共享表，只共享设计模式与代码**。例如 inventory-service 的 `InventoryBucket`：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_bucket")
public class InventoryBucket extends InventoryBucketBase {
    private Long scenicId; // 门票域特有字段
}
```

酒店 `room_inventory_bucket`、会议室 `meeting_room_inventory_bucket` 等未来模块同理，各自建表 + 继承基类，
接入 06 文档「Redis 预扣 + DB 条件更新」两道防线时复用同一套 Lua 脚本与状态机定义。

## 错误码约定

各服务在自己的 `common`/`error` 包内定义实现 `ErrorCode` 的枚举，命名形如 `{SERVICE}_{ERROR_NAME}`
（如 `PRODUCT_SKU_NOT_FOUND`、`INVENTORY_INSUFFICIENT_STOCK`），抛出 `new BizException(MyErrorCode.XXX)`，
`GlobalExceptionHandler` 会自动转换为 09 文档 1.1 的响应格式与 HTTP 状态码。

## 多租户

`RequestContextFilter` 在请求入口读取 `X-Tenant-Id` 头写入 `TenantContext`；
`TenantLineHandlerImpl` 在 MyBatis-Plus 执行 SQL 时自动追加 `tenant_id = ?`。
**本地用 curl/Postman 调试时务必带上 `X-Tenant-Id` 头**，否则租户拦截器会用默认值 `0` 查询，查不到数据。

## 范围说明

当前仅包含门票全链路（07-10 文档）所需的共享代码。本期暂不包含业务 controller/service 实现——
这些在各服务模块中按 README 第 6 步「黄金路径」逐步补齐。
