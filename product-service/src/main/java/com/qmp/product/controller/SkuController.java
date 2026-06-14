package com.qmp.product.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.product.dto.SkuInfoResponse;
import com.qmp.product.service.SkuQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品中心对外接口（见 09 文档二）。
 */
@RestController
@RequestMapping("/api/v1/skus")
@RequiredArgsConstructor
public class SkuController {

    private final SkuQueryService skuQueryService;

    @GetMapping("/{skuId}")
    public ApiResponse<SkuInfoResponse> getSku(@PathVariable Long skuId) {
        return ApiResponse.ok(skuQueryService.getSkuInfo(skuId));
    }
}
