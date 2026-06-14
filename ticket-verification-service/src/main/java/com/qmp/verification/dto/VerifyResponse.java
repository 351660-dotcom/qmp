package com.qmp.verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 核验响应（09 文档七）：{@code { result: SUCCESS, credential_id }}。
 */
@Getter
@Builder
public class VerifyResponse {

    /** SUCCESS（失败场景通过 BizException + HTTP 状态码返回）。 */
    private String result;

    @JsonProperty("credential_id")
    private Long credentialId;
}
