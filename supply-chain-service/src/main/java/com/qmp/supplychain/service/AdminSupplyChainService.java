package com.qmp.supplychain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.supplychain.dto.admin.CreateWarehouseRequest;
import com.qmp.supplychain.entity.Warehouse;
import com.qmp.supplychain.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 供应链后台管理：维护仓库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSupplyChainService {

    private final WarehouseMapper warehouseMapper;

    public Long createWarehouse(CreateWarehouseRequest request) {
        Warehouse warehouse = new Warehouse();
        warehouse.setTenantId(TenantContext.get());
        warehouse.setOwnerScope(request.getOwnerScope());
        warehouse.setMerchantId(request.getMerchantId());
        warehouse.setName(request.getName());
        warehouseMapper.insert(warehouse);
        log.info("后台建仓: warehouseId={}, scope={}, merchantId={}",
                warehouse.getWarehouseId(), request.getOwnerScope(), request.getMerchantId());
        return warehouse.getWarehouseId();
    }

    public List<Warehouse> listWarehouses() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
                .orderByDesc(Warehouse::getWarehouseId));
    }
}
