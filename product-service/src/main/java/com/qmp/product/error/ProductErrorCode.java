package com.qmp.product.error;

import com.qmp.kernel.common.ErrorCode;

/**
 * product-service 错误码（见 09 文档二）。
 */
public enum ProductErrorCode implements ErrorCode {

    SKU_NOT_FOUND(404, "PRODUCT_SKU_NOT_FOUND", "票种不存在"),
    SKU_NOT_ON_SALE(409, "PRODUCT_SKU_NOT_ON_SALE", "票种未上架");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    ProductErrorCode(int httpStatus, String code, String defaultMessage) {
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
