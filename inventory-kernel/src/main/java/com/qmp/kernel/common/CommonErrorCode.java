package com.qmp.kernel.common;

/**
 * 全平台通用错误码（非具体业务域），见 09 文档 1.1 的 400/500 通用语义。
 * 各业务域的资源不存在（404）/状态冲突（409）错误码在各服务内自行定义（如 {@code PRODUCT_SKU_NOT_FOUND}）。
 */
public enum CommonErrorCode implements ErrorCode {

    INVALID_PARAM(400, "COMMON_INVALID_PARAM", "参数校验失败"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "系统内部错误");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    CommonErrorCode(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
