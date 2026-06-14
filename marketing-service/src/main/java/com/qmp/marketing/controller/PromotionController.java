package com.qmp.marketing.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.marketing.dto.PromotionCalcRequest;
import com.qmp.marketing.dto.PromotionCalcResponse;
import com.qmp.marketing.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 营销规则引擎对外接口（13 文档三）。
 */
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /** 试算优惠（DISCOUNT→FULL_REDUCTION），返回命中规则与应付。供下单时快照。 */
    @PostMapping("/calculate")
    public ApiResponse<PromotionCalcResponse> calculate(@RequestBody PromotionCalcRequest request) {
        return ApiResponse.ok(promotionService.calculate(request));
    }
}
