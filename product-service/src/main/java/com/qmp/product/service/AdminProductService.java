package com.qmp.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.product.dto.SkuInfoResponse;
import com.qmp.product.dto.admin.CreateProductRequest;
import com.qmp.product.dto.admin.CreateSkuRequest;
import com.qmp.product.dto.admin.ProductView;
import com.qmp.product.entity.TicketProduct;
import com.qmp.product.entity.TicketSku;
import com.qmp.product.error.ProductErrorCode;
import com.qmp.product.mapper.TicketProductMapper;
import com.qmp.product.mapper.TicketSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 商品中心后台管理服务：创建商品/票种、上下架、列表查询。
 * 让门票主数据可由后台维护，替代 Flyway 种子（见 product-service CLAUDE.md「后台管理」）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private static final Set<String> VALID_STATUS =
            Set.of("DRAFT", "PENDING_REVIEW", "ON_SALE", "OFF_SALE");

    private final TicketProductMapper ticketProductMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final ObjectMapper objectMapper;

    public Long createProduct(CreateProductRequest request) {
        String status = request.getStatus() != null ? request.getStatus() : "DRAFT";
        if (!VALID_STATUS.contains(status)) {
            throw new BizException(ProductErrorCode.INVALID_STATUS);
        }
        TicketProduct product = new TicketProduct();
        product.setTenantId(TenantContext.get());
        product.setScenicId(request.getScenicId());
        product.setMerchantId(request.getMerchantId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setStatus(status);
        product.setValidPeriodRule(request.getValidPeriodRule() != null
                ? request.getValidPeriodRule().toString() : null);
        product.setRealNameRule(request.getRealNameRule());
        ticketProductMapper.insert(product);
        log.info("后台创建商品: productId={}, name={}", product.getProductId(), product.getName());
        return product.getProductId();
    }

    public Long createSku(CreateSkuRequest request) {
        TicketProduct product = ticketProductMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BizException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
        TicketSku sku = new TicketSku();
        sku.setTenantId(TenantContext.get());
        sku.setProductId(request.getProductId());
        sku.setTicketType(request.getTicketType());
        sku.setRequiresTimeSlot(Boolean.TRUE.equals(request.getRequiresTimeSlot()));
        sku.setTimeSlotDefinitions(writeJson(request.getTimeSlotDefinitions()));
        ticketSkuMapper.insert(sku);
        log.info("后台创建票种: skuId={}, productId={}", sku.getSkuId(), request.getProductId());
        return sku.getSkuId();
    }

    public void updateStatus(Long productId, String status) {
        if (!VALID_STATUS.contains(status)) {
            throw new BizException(ProductErrorCode.INVALID_STATUS);
        }
        TicketProduct product = ticketProductMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
        product.setStatus(status);
        ticketProductMapper.updateById(product);
        log.info("后台更新商品状态: productId={}, status={}", productId, status);
    }

    public List<ProductView> listProducts() {
        return ticketProductMapper.selectList(new LambdaQueryWrapper<TicketProduct>()
                        .orderByDesc(TicketProduct::getProductId)).stream()
                .map(p -> ProductView.builder()
                        .productId(p.getProductId())
                        .name(p.getName())
                        .scenicId(p.getScenicId())
                        .merchantId(p.getMerchantId())
                        .status(p.getStatus())
                        .realNameRule(p.getRealNameRule())
                        .build())
                .toList();
    }

    public List<SkuInfoResponse> listSkus(Long productId) {
        TicketProduct product = ticketProductMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
        return ticketSkuMapper.selectList(new LambdaQueryWrapper<TicketSku>()
                        .eq(TicketSku::getProductId, productId)).stream()
                .map(sku -> SkuInfoResponse.builder()
                        .skuId(sku.getSkuId())
                        .productId(product.getProductId())
                        .scenicId(product.getScenicId())
                        .merchantId(product.getMerchantId())
                        .status(product.getStatus())
                        .ticketType(sku.getTicketType())
                        .realNameRule(product.getRealNameRule())
                        .requiresTimeSlot(Boolean.TRUE.equals(sku.getRequiresTimeSlot()))
                        .timeSlotDefinitions(readJsonList(sku.getTimeSlotDefinitions()))
                        .build())
                .toList();
    }

    private String writeJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new IllegalStateException("序列化 time_slot_definitions 失败", e);
        }
    }

    private List<String> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
