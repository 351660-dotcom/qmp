package com.qmp.verification.error;

import com.qmp.kernel.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 核验中心错误码（09 文档七）。命名 {@code VERIFICATION_{ERROR_NAME}}，与 HTTP 状态码联动。
 */
@Getter
@RequiredArgsConstructor
public enum VerificationErrorCode implements ErrorCode {

    CREDENTIAL_NOT_FOUND(404, "VERIFICATION_CREDENTIAL_NOT_FOUND", "核销凭证不存在"),
    INVALID_SIGNATURE(400, "VERIFICATION_INVALID_SIGNATURE", "核销凭证签名无效"),
    ALREADY_VERIFIED(409, "VERIFICATION_ALREADY_VERIFIED", "该凭证已核销"),
    EXPIRED(409, "VERIFICATION_EXPIRED", "该凭证已过期"),
    /** REFUNDING/REFUNDED/VOIDED 等非可核销状态尝试核验（09 文档七 409 的本服务扩展取值）。 */
    NOT_VERIFIABLE(409, "VERIFICATION_NOT_VERIFIABLE", "该凭证当前状态不可核销"),
    REFUND_NOT_ALLOWED(409, "VERIFICATION_REFUND_NOT_ALLOWED", "该凭证不可退票（已核销或超出可退窗口）");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}
