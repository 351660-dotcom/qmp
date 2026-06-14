package com.qmp.pricing.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.pricing.dto.PriceResponse;
import com.qmp.pricing.dto.admin.UpsertPriceRequest;
import com.qmp.pricing.service.AdminPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 价格中心后台管理接口（{@code /admin/v1}）。后台据此维护门市价/会员价。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminPriceController {

    private final AdminPriceService adminPriceService;

    @PutMapping("/prices")
    public ApiResponse<Map<String, Object>> upsert(@RequestBody UpsertPriceRequest request) {
        return ApiResponse.ok(Map.of("price_calendar_id", adminPriceService.upsertPrice(request)));
    }

    @GetMapping("/prices")
    public ApiResponse<List<PriceResponse>> list(
            @RequestParam("sku_id") Long skuId,
            @RequestParam(value = "sale_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate) {
        return ApiResponse.ok(adminPriceService.listPrices(skuId, saleDate));
    }
}
