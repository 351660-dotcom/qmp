package com.qmp.inventory.error;

import com.qmp.kernel.common.ErrorCode;

/**
 * inventory-service 错误码（见 09 文档五）。
 */
public enum InventoryErrorCode implements ErrorCode {

    BUCKET_NOT_FOUND(404, "INVENTORY_BUCKET_NOT_FOUND", "未找到对应的库存配置"),
    INSUFFICIENT_STOCK(409, "INVENTORY_INSUFFICIENT_STOCK", "库存不足"),
    RESERVATION_NOT_FOUND(404, "INVENTORY_RESERVATION_NOT_FOUND", "预占记录不存在"),
    RESERVATION_INVALID_STATE(409, "INVENTORY_RESERVATION_INVALID_STATE", "预占记录状态不允许该操作");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    InventoryErrorCode(int httpStatus, String code, String defaultMessage) {
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
