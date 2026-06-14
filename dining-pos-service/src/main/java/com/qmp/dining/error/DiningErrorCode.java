package com.qmp.dining.error;

import com.qmp.kernel.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 餐饮 POS 错误码（12 文档）。
 */
@Getter
@RequiredArgsConstructor
public enum DiningErrorCode implements ErrorCode {

    TABLE_NOT_FOUND(404, "DINING_TABLE_NOT_FOUND", "桌台不存在"),
    TABLE_NOT_IDLE(409, "DINING_TABLE_NOT_IDLE", "桌台非空闲，无法开台"),
    TABLE_ORDER_NOT_FOUND(404, "DINING_TABLE_ORDER_NOT_FOUND", "台账不存在"),
    TABLE_ORDER_NOT_OPEN(409, "DINING_TABLE_ORDER_NOT_OPEN", "台账非进行中，不可操作"),
    ORDER_LINE_NOT_FOUND(404, "DINING_ORDER_LINE_NOT_FOUND", "点单项不存在"),
    LINE_INVALID_STATE(409, "DINING_LINE_INVALID_STATE", "点单项状态不允许该操作"),
    DISH_SOLD_OUT(409, "DINING_DISH_SOLD_OUT", "菜品已沽清"),
    CHECKOUT_LINE_NOT_READY(409, "DINING_CHECKOUT_LINE_NOT_READY", "存在未完成出品的点单项，不可结账");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}
