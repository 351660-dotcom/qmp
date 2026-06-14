package com.qmp.supplychain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.supplychain.dto.admin.UpsertBomRequest;
import com.qmp.supplychain.entity.DishBom;
import com.qmp.supplychain.mapper.DishBomMapper;
import com.qmp.supplychain.mapper.SkuStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 菜品 BOM 服务（12 文档六）：维护用料表、按销量核减门店库存（POS 销售自动核减）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BomService {

    private final DishBomMapper bomMapper;
    private final SkuStockMapper skuStockMapper;
    private final ObjectMapper objectMapper;

    public Long upsertBom(UpsertBomRequest request) {
        DishBom existing = bomMapper.selectOne(new LambdaQueryWrapper<DishBom>()
                .eq(DishBom::getOutputSkuId, request.getOutputSkuId()).last("LIMIT 1"));
        String materials = request.getMaterials() != null ? request.getMaterials().toString() : "[]";
        if (existing != null) {
            existing.setOutputQuantity(request.getOutputQuantity());
            existing.setMaterials(materials);
            bomMapper.updateById(existing);
            return existing.getBomId();
        }
        DishBom bom = new DishBom();
        bom.setTenantId(TenantContext.get());
        bom.setOutputSkuId(request.getOutputSkuId());
        bom.setOutputQuantity(request.getOutputQuantity() != null ? request.getOutputQuantity() : BigDecimal.ONE);
        bom.setMaterials(materials);
        bomMapper.insert(bom);
        log.info("设置 BOM: outputSkuId={}", request.getOutputSkuId());
        return bom.getBomId();
    }

    /**
     * 按销量核减门店库存（12 文档 6.3）：原料核减量 = 用量 × 销量 / 产出量；允许临时为负。
     * 未配置 BOM 或库存记录缺失仅记录异常，不阻塞（由库管补录后重放）。
     */
    public void deductForDish(Long warehouseId, Long outputSkuId, int soldQty) {
        DishBom bom = bomMapper.selectOne(new LambdaQueryWrapper<DishBom>()
                .eq(DishBom::getOutputSkuId, outputSkuId).last("LIMIT 1"));
        if (bom == null) {
            log.warn("未配置 BOM，跳过核减: outputSkuId={}", outputSkuId);
            return;
        }
        BigDecimal outputQty = bom.getOutputQuantity() != null && bom.getOutputQuantity().signum() > 0
                ? bom.getOutputQuantity() : BigDecimal.ONE;
        try {
            JsonNode materials = objectMapper.readTree(bom.getMaterials());
            for (JsonNode m : materials) {
                Long materialSkuId = m.get("material_sku_id").asLong();
                BigDecimal perUnit = m.get("quantity").decimalValue();
                BigDecimal deduct = perUnit.multiply(BigDecimal.valueOf(soldQty))
                        .divide(outputQty, 4, java.math.RoundingMode.HALF_UP);
                int rows = skuStockMapper.deductAllowNegative(warehouseId, materialSkuId, deduct);
                if (rows == 0) {
                    log.warn("门店仓无该原料库存记录，核减异常待补录: warehouseId={}, materialSkuId={}",
                            warehouseId, materialSkuId);
                }
            }
        } catch (Exception e) {
            log.error("BOM 核减解析失败: outputSkuId={}, materials={}", outputSkuId, bom.getMaterials(), e);
        }
    }
}
