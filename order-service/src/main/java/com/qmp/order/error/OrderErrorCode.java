package com.qmp.order.error;

import com.qmp.kernel.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单编排错误码（09 文档八）。其中 {@code PRODUCT_SKU_NOT_ON_SALE}/{@code INVENTORY_INSUFFICIENT_STOCK}
 * 沿用下游域错误码语义（编排失败时对外透出）。
 */
@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(404, "ORDER_NOT_FOUND", "订单不存在"),
    SKU_NOT_ON_SALE(409, "PRODUCT_SKU_NOT_ON_SALE", "票种未上架，不可下单"),
    INSUFFICIENT_STOCK(409, "INVENTORY_INSUFFICIENT_STOCK", "库存不足"),
    ORDER_INVALID_STATE(409, "ORDER_INVALID_STATE", "订单状态不允许该操作"),
    ORDER_PAY_EXPIRED(409, "ORDER_PAY_EXPIRED", "订单已超时关闭，不可支付"),
    UPSTREAM_ERROR(502, "ORDER_UPSTREAM_ERROR", "下游服务调用失败");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}
