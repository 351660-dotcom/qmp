package com.qmp.marketing.error;

import com.qmp.kernel.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 营销中心错误码（13 文档）。
 */
@Getter
@RequiredArgsConstructor
public enum MarketingErrorCode implements ErrorCode {

    TEMPLATE_NOT_FOUND(404, "MARKETING_TEMPLATE_NOT_FOUND", "优惠券模板不存在"),
    COUPON_NOT_FOUND(404, "MARKETING_COUPON_NOT_FOUND", "优惠券不存在"),
    COUPON_NOT_USABLE(409, "MARKETING_COUPON_NOT_USABLE", "优惠券当前状态不可核销"),
    ISSUE_QUOTA_EXCEEDED(409, "MARKETING_ISSUE_QUOTA_EXCEEDED", "优惠券发放已达上限");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}
