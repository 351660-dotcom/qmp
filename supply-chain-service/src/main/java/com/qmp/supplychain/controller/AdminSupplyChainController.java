package com.qmp.supplychain.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.supplychain.dto.admin.CreateWarehouseRequest;
import com.qmp.supplychain.dto.admin.UpsertBomRequest;
import com.qmp.supplychain.dto.admin.UpsertStockRequest;
import com.qmp.supplychain.entity.Warehouse;
import com.qmp.supplychain.service.AdminSupplyChainService;
import com.qmp.supplychain.service.BomService;
import com.qmp.supplychain.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 供应链后台管理接口（{@code /admin/v1}）：仓库、库存、BOM。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminSupplyChainController {

    private final AdminSupplyChainService adminSupplyChainService;
    private final StockService stockService;
    private final BomService bomService;

    @PostMapping("/warehouses")
    public ApiResponse<Map<String, Object>> createWarehouse(@RequestBody CreateWarehouseRequest request) {
        return ApiResponse.ok(Map.of("warehouse_id", adminSupplyChainService.createWarehouse(request)));
    }

    @GetMapping("/warehouses")
    public ApiResponse<List<Warehouse>> listWarehouses() {
        return ApiResponse.ok(adminSupplyChainService.listWarehouses());
    }

    @PostMapping("/stocks")
    public ApiResponse<Map<String, Object>> upsertStock(@RequestBody UpsertStockRequest request) {
        return ApiResponse.ok(Map.of("stock_id", stockService.upsertStock(request)));
    }

    @PostMapping("/boms")
    public ApiResponse<Map<String, Object>> upsertBom(@RequestBody UpsertBomRequest request) {
        return ApiResponse.ok(Map.of("bom_id", bomService.upsertBom(request)));
    }
}
