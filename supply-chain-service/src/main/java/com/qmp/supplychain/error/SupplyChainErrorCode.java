package com.qmp.supplychain.error;

import com.qmp.kernel.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 供应链错误码（12 文档五/六）。
 */
@Getter
@RequiredArgsConstructor
public enum SupplyChainErrorCode implements ErrorCode {

    WAREHOUSE_NOT_FOUND(404, "SUPPLY_WAREHOUSE_NOT_FOUND", "仓库不存在"),
    STOCK_NOT_FOUND(404, "SUPPLY_STOCK_NOT_FOUND", "库存记录不存在"),
    INSUFFICIENT_STOCK(409, "SUPPLY_INSUFFICIENT_STOCK", "库存不足");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}
