package com.qmp.pricing.error;

import com.qmp.kernel.common.ErrorCode;

/**
 * pricing-service 错误码（见 09 文档三）。
 */
public enum PricingErrorCode implements ErrorCode {

    PRICE_NOT_CONFIGURED(404, "PRICING_NOT_CONFIGURED", "该票种当日未配置门市价");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    PricingErrorCode(int httpStatus, String code, String defaultMessage) {
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
