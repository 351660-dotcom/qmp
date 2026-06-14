package com.qmp.supplychain.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.supplychain.entity.SkuStock;
import com.qmp.supplychain.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存查询接口（12 文档 5.2）。
 */
@RestController
@RequestMapping("/api/v1/supply")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/stocks")
    public ApiResponse<SkuStock> getStock(@RequestParam("warehouse_id") Long warehouseId,
                                          @RequestParam("sku_id") Long skuId) {
        return ApiResponse.ok(stockService.getStock(warehouseId, skuId));
    }
}
