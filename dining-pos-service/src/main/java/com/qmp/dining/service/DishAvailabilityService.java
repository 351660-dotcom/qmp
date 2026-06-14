package com.qmp.dining.service;

import com.qmp.dining.entity.DishAvailability;
import com.qmp.dining.mapper.DishAvailabilityMapper;
import com.qmp.kernel.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 菜品沽清（12 文档 2.2）：标记/恢复售卖状态、查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishAvailabilityService {

    private final DishAvailabilityMapper availabilityMapper;

    /** 标记沽清/恢复（upsert）。status: AVAILABLE/SOLD_OUT。 */
    public void setStatus(Long skuId, Long merchantId, String status) {
        DishAvailability existing = availabilityMapper.selectById(skuId);
        if (existing != null) {
            existing.setStatus(status);
            availabilityMapper.updateById(existing);
        } else {
            DishAvailability da = new DishAvailability();
            da.setSkuId(skuId);
            da.setTenantId(TenantContext.get());
            da.setMerchantId(merchantId);
            da.setStatus(status);
            availabilityMapper.insert(da);
        }
        log.info("菜品沽清状态: skuId={}, status={}", skuId, status);
    }

    public boolean isSoldOut(Long skuId) {
        DishAvailability da = availabilityMapper.selectById(skuId);
        return da != null && "SOLD_OUT".equals(da.getStatus());
    }

    public DishAvailability get(Long skuId) {
        return availabilityMapper.selectById(skuId);
    }
}
