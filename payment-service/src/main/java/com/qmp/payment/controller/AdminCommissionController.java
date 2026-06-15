package com.qmp.payment.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.payment.dto.admin.UpsertCommissionRequest;
import com.qmp.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台分账配置接口（{@code /admin/v1}，由 inventory-kernel AdminAuthFilter 保护）。
 * 每个商户可设不同的平台抽成比例，供分账时计算 platform/merchant 金额。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminCommissionController {

    private final PaymentService paymentService;

    @PutMapping("/merchant-commissions")
    public ApiResponse<Void> upsert(@RequestBody UpsertCommissionRequest request) {
        paymentService.upsertCommission(request.getMerchantId(), request.getCommissionRate());
        return ApiResponse.ok();
    }
}
