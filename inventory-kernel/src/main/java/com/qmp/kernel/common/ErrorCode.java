package com.qmp.kernel.common;

/**
 * 业务错误码契约（见 09 文档 1.1）：{@code code} 形如 {@code {SERVICE}_{ERROR_NAME}}（大写 snake case），
 * 与 HTTP 状态码联动。各服务在自己的包内定义实现该接口的枚举，例如：
 *
 * <pre>{@code
 * public enum ProductErrorCode implements ErrorCode {
 *     SKU_NOT_FOUND(404, "PRODUCT_SKU_NOT_FOUND", "票种不存在");
 *     ...
 * }
 * }</pre>
 */
public interface ErrorCode {

    /** 业务错误码，格式 {@code {SERVICE}_{ERROR_NAME}}。 */
    String getCode();

    /** 联动的 HTTP 状态码：400 参数错误 / 404 资源不存在 / 409 状态冲突 / 500 系统错误。 */
    int getHttpStatus();

    /** 默认错误信息。 */
    String getDefaultMessage();
}
