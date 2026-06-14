package com.qmp.kernel.common;

import lombok.Getter;

/**
 * 业务异常：携带 {@link ErrorCode}，由 {@link com.qmp.kernel.web.GlobalExceptionHandler}
 * 统一转换为 {@link ApiResponse#error(ErrorCode, String)} 并联动 HTTP 状态码。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
