package com.qmp.marketing.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.marketing.dto.admin.CreateTemplateRequest;
import com.qmp.marketing.entity.CouponTemplate;
import com.qmp.marketing.service.AdminMarketingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 营销后台管理接口（{@code /admin/v1}）。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminMarketingController {

    private final AdminMarketingService adminMarketingService;

    @PostMapping("/coupon-templates")
    public ApiResponse<Map<String, Object>> createTemplate(@RequestBody CreateTemplateRequest request) {
        return ApiResponse.ok(Map.of("template_id", adminMarketingService.createTemplate(request)));
    }

    @GetMapping("/coupon-templates")
    public ApiResponse<List<CouponTemplate>> listTemplates() {
        return ApiResponse.ok(adminMarketingService.listTemplates());
    }
}
