package com.qmp.product.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.product.dto.SkuInfoResponse;
import com.qmp.product.dto.admin.CreateProductRequest;
import com.qmp.product.dto.admin.CreateSkuRequest;
import com.qmp.product.dto.admin.ProductView;
import com.qmp.product.dto.admin.UpdateProductStatusRequest;
import com.qmp.product.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 商品中心后台管理接口（{@code /admin/v1}）。后台据此维护商品/票种主数据与上下架。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @PostMapping("/products")
    public ApiResponse<Map<String, Object>> createProduct(@RequestBody CreateProductRequest request) {
        return ApiResponse.ok(Map.of("product_id", adminProductService.createProduct(request)));
    }

    @PatchMapping("/products/{productId}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long productId,
                                          @RequestBody UpdateProductStatusRequest request) {
        adminProductService.updateStatus(productId, request.getStatus());
        return ApiResponse.ok();
    }

    @GetMapping("/products")
    public ApiResponse<List<ProductView>> listProducts() {
        return ApiResponse.ok(adminProductService.listProducts());
    }

    @PostMapping("/skus")
    public ApiResponse<Map<String, Object>> createSku(@RequestBody CreateSkuRequest request) {
        return ApiResponse.ok(Map.of("sku_id", adminProductService.createSku(request)));
    }

    @GetMapping("/skus")
    public ApiResponse<List<SkuInfoResponse>> listSkus(@RequestParam("product_id") Long productId) {
        return ApiResponse.ok(adminProductService.listSkus(productId));
    }
}
