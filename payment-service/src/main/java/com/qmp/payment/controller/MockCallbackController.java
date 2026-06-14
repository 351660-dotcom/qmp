package com.qmp.payment.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.payment.dto.MockCallbackRequest;
import com.qmp.payment.dto.PaymentResponse;
import com.qmp.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v1 模拟支付渠道回调（替代 09 文档 6.1 的微信/支付宝回调，见本服务 CLAUDE.md）。
 * 仅用于本地/测试环境驱动门票黄金路径，不对外暴露 OpenAPI。
 */
@RestController
@RequestMapping("/internal/callbacks")
@RequiredArgsConstructor
public class MockCallbackController {

    private final PaymentService paymentService;

    @PostMapping("/mock")
    public ApiResponse<PaymentResponse> mockCallback(@RequestBody MockCallbackRequest request) {
        return ApiResponse.ok(paymentService.handleMockCallback(request));
    }
}
