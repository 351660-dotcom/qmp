package com.qmp.inventory.controller;

import com.qmp.inventory.dto.admin.UpsertBucketRequest;
import com.qmp.inventory.entity.InventoryBucket;
import com.qmp.inventory.service.AdminInventoryService;
import com.qmp.kernel.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 库存中心后台管理接口（{@code /admin/v1}）。后台据此创建/调整库存桶配额。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final AdminInventoryService adminInventoryService;

    @PostMapping("/buckets")
    public ApiResponse<Map<String, Object>> upsert(@RequestBody UpsertBucketRequest request) {
        return ApiResponse.ok(Map.of("bucket_id", adminInventoryService.upsertBucket(request)));
    }

    @GetMapping("/buckets")
    public ApiResponse<InventoryBucket> get(
            @RequestParam("sku_id") Long skuId,
            @RequestParam("sale_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate,
            @RequestParam(value = "time_slot_id", required = false) Long timeSlotId) {
        return ApiResponse.ok(adminInventoryService.getBucket(skuId, saleDate, timeSlotId));
    }
}
