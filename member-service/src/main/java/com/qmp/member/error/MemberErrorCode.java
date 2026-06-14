package com.qmp.member.error;

import com.qmp.kernel.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 会员中心错误码（13 文档）。
 */
@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    INSUFFICIENT_POINTS(409, "MEMBER_INSUFFICIENT_POINTS", "积分余额不足"),
    INSUFFICIENT_BALANCE(409, "MEMBER_INSUFFICIENT_BALANCE", "储值余额不足"),
    INVALID_AMOUNT(400, "MEMBER_INVALID_AMOUNT", "金额/积分必须为正");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}
