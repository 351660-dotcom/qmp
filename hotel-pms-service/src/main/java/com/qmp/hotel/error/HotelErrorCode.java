package com.qmp.hotel.error;

import com.qmp.kernel.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 酒店 PMS 错误码（11 文档）。命名 {@code HOTEL_{ERROR_NAME}}。
 */
@Getter
@RequiredArgsConstructor
public enum HotelErrorCode implements ErrorCode {

    ROOM_TYPE_NOT_FOUND(404, "HOTEL_ROOM_TYPE_NOT_FOUND", "房型不存在"),
    ROOM_TYPE_NOT_ON_SALE(409, "HOTEL_ROOM_TYPE_NOT_ON_SALE", "房型未上架"),
    BUCKET_NOT_FOUND(404, "HOTEL_BUCKET_NOT_FOUND", "该房型当晚未配置房晚库存"),
    INSUFFICIENT_ROOM(409, "HOTEL_INSUFFICIENT_ROOM", "房晚库存不足"),
    RESERVATION_NOT_FOUND(404, "HOTEL_RESERVATION_NOT_FOUND", "预订单不存在"),
    RESERVATION_INVALID_STATE(409, "HOTEL_RESERVATION_INVALID_STATE", "预订单状态不允许该操作"),
    INVALID_DATE_RANGE(400, "HOTEL_INVALID_DATE_RANGE", "入离日期非法"),
    INVALID_QUOTA(400, "HOTEL_INVALID_QUOTA", "总配额不得小于已售+已锁定数量");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}
