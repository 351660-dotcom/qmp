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
    ISSUE_QUOTA_EXCEEDED(409, "MARKETING_ISSUE_QUOTA_EXCEEDED", "优惠券发放已达上限"),
    SECKILL_NOT_FOUND(404, "MARKETING_SECKILL_NOT_FOUND", "秒杀活动不存在"),
    SECKILL_NOT_ACTIVE(409, "MARKETING_SECKILL_NOT_ACTIVE", "秒杀活动未在进行中"),
    SECKILL_BUCKET_NOT_FOUND(404, "MARKETING_SECKILL_BUCKET_NOT_FOUND", "秒杀库存未配置"),
    SECKILL_SOLD_OUT(409, "MARKETING_SECKILL_SOLD_OUT", "秒杀名额已抢完"),
    SECKILL_ALREADY_SNAPPED(409, "MARKETING_SECKILL_ALREADY_SNAPPED", "该用户已抢购过（限购1）"),
    INVALID_QUOTA(400, "MARKETING_INVALID_QUOTA", "秒杀配额非法");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}
