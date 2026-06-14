package com.qmp.kernel.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qmp.kernel.context.TraceContext;
import lombok.Getter;

/**
 * 全平台统一 HTTP 响应格式（见 09 文档 1.1）。
 *
 * <pre>{@code
 * {
 *   "code": "OK",
 *   "message": "success",
 *   "data": { },
 *   "trace_id": "a1b2c3d4"
 * }
 * }</pre>
 */
@Getter
public class ApiResponse<T> {

    public static final String OK_CODE = "OK";

    private final String code;
    private final String message;
    private final T data;

    @JsonProperty("trace_id")
    private final String traceId;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = TraceContext.get();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(OK_CODE, "success", data);
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getDefaultMessage());
    }
}
