package com.qmp.verification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 出票响应（09 文档七）：{@code { credentials: [...] }}。
 */
@Getter
@Builder
public class IssueCredentialsResponse {

    private List<CredentialDto> credentials;
}
