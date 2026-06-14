package com.qmp.verification.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.verification.dto.IssueCredentialsRequest;
import com.qmp.verification.dto.IssueCredentialsResponse;
import com.qmp.verification.dto.RefundRequestResponse;
import com.qmp.verification.dto.VerifyRequest;
import com.qmp.verification.dto.VerifyResponse;
import com.qmp.verification.service.CredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 核验中心对外接口（09 文档七）。
 */
@RestController
@RequestMapping("/api/v1/credentials")
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialService credentialService;

    /** 出票，幂等键 = order_item_id。 */
    @PostMapping
    public ApiResponse<IssueCredentialsResponse> issue(@RequestBody IssueCredentialsRequest request) {
        return ApiResponse.ok(credentialService.issueCredentials(request));
    }

    /** 核验（线上）。 */
    @PostMapping("/verify")
    public ApiResponse<VerifyResponse> verify(@RequestBody VerifyRequest request) {
        return ApiResponse.ok(credentialService.verify(request));
    }

    /** 申请退票，内部同步调用 payment-service 创建退款。 */
    @PostMapping("/{credentialId}/refund-request")
    public ApiResponse<RefundRequestResponse> refundRequest(@PathVariable Long credentialId) {
        return ApiResponse.ok(credentialService.requestRefund(credentialId));
    }
}
