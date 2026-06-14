package com.qmp.dining.controller;

import com.qmp.dining.dto.DishStatusRequest;
import com.qmp.dining.entity.DishAvailability;
import com.qmp.dining.service.DishAvailabilityService;
import com.qmp.kernel.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜品沽清接口（12 文档 2.2）。
 */
@RestController
@RequestMapping("/api/v1/dining/dishes")
@RequiredArgsConstructor
public class DishAvailabilityController {

    private final DishAvailabilityService dishAvailabilityService;

    @PutMapping("/availability")
    public ApiResponse<Void> setStatus(@RequestBody DishStatusRequest request) {
        dishAvailabilityService.setStatus(request.getSkuId(), request.getMerchantId(), request.getStatus());
        return ApiResponse.ok();
    }

    @GetMapping("/{skuId}/availability")
    public ApiResponse<DishAvailability> get(@PathVariable Long skuId) {
        return ApiResponse.ok(dishAvailabilityService.get(skuId));
    }
}
