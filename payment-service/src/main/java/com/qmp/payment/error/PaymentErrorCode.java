package com.qmp.payment.error;

import com.qmp.kernel.common.ErrorCode;

/**
 * payment-service 错误码（见 09 文档六）。
 */
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(404, "PAYMENT_NOT_FOUND", "支付单不存在"),
    PAYMENT_ALREADY_PAID(409, "PAYMENT_ALREADY_PAID", "支付已成功，订单侧应转为正常流程而非报错");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    PaymentErrorCode(int httpStatus, String code, String defaultMessage) {
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
