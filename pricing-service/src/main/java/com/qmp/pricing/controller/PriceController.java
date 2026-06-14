package com.qmp.pricing.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.pricing.dto.PriceResponse;
import com.qmp.pricing.service.PriceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 价格中心对外接口（见 09 文档三）。
 */
@RestController
@RequestMapping("/api/v1/price")
@RequiredArgsConstructor
public class PriceController {

    private final PriceQueryService priceQueryService;

    @GetMapping
    public ApiResponse<PriceResponse> getPrice(
            @RequestParam("sku_id") Long skuId,
            @RequestParam("sale_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate,
            @RequestParam("is_member") boolean isMember) {
        return ApiResponse.ok(priceQueryService.getPrice(skuId, saleDate, isMember));
    }
}
