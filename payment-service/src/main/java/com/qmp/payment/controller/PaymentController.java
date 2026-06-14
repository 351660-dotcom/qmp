package com.qmp.payment.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.payment.dto.CreatePaymentRequest;
import com.qmp.payment.dto.CreateRefundRequest;
import com.qmp.payment.dto.PaymentResponse;
import com.qmp.payment.dto.RefundResponse;
import com.qmp.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付分账中心对外接口（见 09 文档六）。
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(@RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok(paymentService.createPayment(request));
    }

    @PostMapping("/{paymentId}/close")
    public ApiResponse<PaymentResponse> closePayment(@PathVariable String paymentId) {
        return ApiResponse.ok(paymentService.closePayment(paymentId));
    }

    @PostMapping("/{paymentId}/refunds")
    public ApiResponse<RefundResponse> createRefund(@PathVariable String paymentId,
                                                      @RequestBody CreateRefundRequest request) {
        return ApiResponse.ok(paymentService.createRefund(paymentId, request));
    }
}
