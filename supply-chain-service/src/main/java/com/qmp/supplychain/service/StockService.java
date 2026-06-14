package com.qmp.supplychain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.supplychain.dto.admin.UpsertStockRequest;
import com.qmp.supplychain.entity.SkuStock;
import com.qmp.supplychain.entity.Warehouse;
import com.qmp.supplychain.error.SupplyChainErrorCode;
import com.qmp.supplychain.mapper.SkuStockMapper;
import com.qmp.supplychain.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 库存服务（12 文档 5.2）：设置/入库/出库/查询，以及按商户定位门店仓。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final SkuStockMapper skuStockMapper;
    private final WarehouseMapper warehouseMapper;

    public Long upsertStock(UpsertStockRequest request) {
        SkuStock existing = find(request.getWarehouseId(), request.getSkuId());
        if (existing != null) {
            existing.setQuantity(request.getQuantity());
            existing.setUnit(request.getUnit());
            existing.setReorderPoint(request.getReorderPoint());
            skuStockMapper.updateById(existing);
            return existing.getStockId();
        }
        SkuStock stock = new SkuStock();
        stock.setTenantId(TenantContext.get());
        stock.setWarehouseId(request.getWarehouseId());
        stock.setSkuId(request.getSkuId());
        stock.setQuantity(request.getQuantity() != null ? request.getQuantity() : BigDecimal.ZERO);
        stock.setUnit(request.getUnit());
        stock.setReorderPoint(request.getReorderPoint());
        stock.setVersion(0);
        skuStockMapper.insert(stock);
        log.info("设置库存: warehouseId={}, skuId={}, qty={}",
                request.getWarehouseId(), request.getSkuId(), request.getQuantity());
        return stock.getStockId();
    }

    public void stockIn(Long warehouseId, Long skuId, BigDecimal delta) {
        if (skuStockMapper.addQuantity(warehouseId, skuId, delta) == 0) {
            throw new BizException(SupplyChainErrorCode.STOCK_NOT_FOUND);
        }
    }

    public void stockOut(Long warehouseId, Long skuId, BigDecimal delta) {
        if (skuStockMapper.deductConditional(warehouseId, skuId, delta) == 0) {
            throw new BizException(SupplyChainErrorCode.INSUFFICIENT_STOCK);
        }
    }

    public SkuStock getStock(Long warehouseId, Long skuId) {
        SkuStock stock = find(warehouseId, skuId);
        if (stock == null) {
            throw new BizException(SupplyChainErrorCode.STOCK_NOT_FOUND);
        }
        return stock;
    }

    /** 定位某餐饮商户的门店仓（owner_scope=STORE）。 */
    public Warehouse findStoreWarehouse(Long merchantId) {
        return warehouseMapper.selectOne(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getOwnerScope, "STORE")
                .eq(Warehouse::getMerchantId, merchantId)
                .last("LIMIT 1"));
    }

    private SkuStock find(Long warehouseId, Long skuId) {
        return skuStockMapper.selectOne(new LambdaQueryWrapper<SkuStock>()
                .eq(SkuStock::getWarehouseId, warehouseId)
                .eq(SkuStock::getSkuId, skuId)
                .last("LIMIT 1"));
    }
}
