package com.qmp.verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 核验请求（09 文档七 {@code POST /api/v1/credentials/verify}）。
 */
@Data
public class VerifyRequest {

    @JsonProperty("verify_code")
    private String verifyCode;

    @JsonProperty("terminal_id")
    private String terminalId;
}
