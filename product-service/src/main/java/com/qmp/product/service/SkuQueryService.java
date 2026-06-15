package com.qmp.product.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.product.dto.SkuInfoResponse;
import com.qmp.product.entity.TicketProduct;
import com.qmp.product.entity.TicketSku;
import com.qmp.product.error.ProductErrorCode;
import com.qmp.product.mapper.TicketProductMapper;
import com.qmp.product.mapper.TicketSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 票种查询（09 文档二），供 order-service 创建订单时校验 SKU 状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuQueryService {

    private final TicketSkuMapper ticketSkuMapper;
    private final TicketProductMapper ticketProductMapper;
    private final ObjectMapper objectMapper;

    public SkuInfoResponse getSkuInfo(Long skuId) {
        TicketSku sku = ticketSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(ProductErrorCode.SKU_NOT_FOUND);
        }
        TicketProduct product = ticketProductMapper.selectById(sku.getProductId());
        if (product == null) {
            throw new BizException(ProductErrorCode.SKU_NOT_FOUND);
        }
        return SkuInfoResponse.builder()
                .skuId(sku.getSkuId())
                .productId(product.getProductId())
                .scenicId(product.getScenicId())
                .merchantId(product.getMerchantId())
                .status(product.getStatus())
                .ticketType(sku.getTicketType())
                .realNameRule(product.getRealNameRule())
                .refundPolicy(product.getRefundPolicy())
                .verificationMedium(parseStringList(product.getVerificationMedium()))
                .requiresTimeSlot(Boolean.TRUE.equals(sku.getRequiresTimeSlot()))
                .timeSlotDefinitions(parseStringList(sku.getTimeSlotDefinitions()))
                .build();
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("解析 time_slot_definitions 失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
