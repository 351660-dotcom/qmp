package com.qmp.verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 单张凭证视图（09 文档七出票响应 {@code credentials[]} 元素）。
 */
@Getter
@Builder
public class CredentialDto {

    @JsonProperty("credential_id")
    private Long credentialId;

    @JsonProperty("ticket_index")
    private Integer ticketIndex;

    @JsonProperty("verify_code")
    private String verifyCode;

    private String status;
}
