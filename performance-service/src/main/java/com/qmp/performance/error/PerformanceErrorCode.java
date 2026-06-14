package com.qmp.performance.error;

import com.qmp.kernel.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 演出/游船/游乐错误码（14 文档）。
 */
@Getter
@RequiredArgsConstructor
public enum PerformanceErrorCode implements ErrorCode {

    SESSION_NOT_FOUND(404, "PERFORMANCE_SESSION_NOT_FOUND", "场次不存在"),
    SESSION_NOT_ON_SALE(409, "PERFORMANCE_SESSION_NOT_ON_SALE", "场次未开售"),
    BUCKET_NOT_FOUND(404, "PERFORMANCE_BUCKET_NOT_FOUND", "未配置库存桶"),
    INSUFFICIENT(409, "PERFORMANCE_INSUFFICIENT", "余位不足"),
    BOOKING_NOT_FOUND(404, "PERFORMANCE_BOOKING_NOT_FOUND", "预订单不存在"),
    BOOKING_INVALID_STATE(409, "PERFORMANCE_BOOKING_INVALID_STATE", "预订单状态不允许该操作"),
    WRISTBAND_NOT_FOUND(404, "PERFORMANCE_WRISTBAND_NOT_FOUND", "手牌账户不存在"),
    INSUFFICIENT_WRISTBAND(409, "PERFORMANCE_INSUFFICIENT_WRISTBAND", "手牌余额不足"),
    INVALID_QUOTA(400, "PERFORMANCE_INVALID_QUOTA", "总配额不得小于已售+已锁定数量"),
    INVALID_AMOUNT(400, "PERFORMANCE_INVALID_AMOUNT", "金额必须为正");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}
