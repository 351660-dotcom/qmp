package com.qmp.supplychain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.supplychain.entity.SkuStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 库存 Mapper。出入库条件更新（12 文档 5.2 / ADR-018）。
 */
@Mapper
public interface SkuStockMapper extends BaseMapper<SkuStock> {

    /** 入库：数量增加。 */
    @Update("UPDATE sku_stock SET quantity = quantity + #{delta}, version = version + 1 "
            + "WHERE warehouse_id = #{warehouseId} AND sku_id = #{skuId}")
    int addQuantity(@Param("warehouseId") Long warehouseId, @Param("skuId") Long skuId,
                    @Param("delta") BigDecimal delta);

    /** 手动出库：条件更新防负（quantity >= delta）。 */
    @Update("UPDATE sku_stock SET quantity = quantity - #{delta}, version = version + 1 "
            + "WHERE warehouse_id = #{warehouseId} AND sku_id = #{skuId} AND quantity >= #{delta}")
    int deductConditional(@Param("warehouseId") Long warehouseId, @Param("skuId") Long skuId,
                          @Param("delta") BigDecimal delta);

    /** POS 按 BOM 核减：允许临时为负（12 文档 5.6/6.3）。 */
    @Update("UPDATE sku_stock SET quantity = quantity - #{delta}, version = version + 1 "
            + "WHERE warehouse_id = #{warehouseId} AND sku_id = #{skuId}")
    int deductAllowNegative(@Param("warehouseId") Long warehouseId, @Param("skuId") Long skuId,
                            @Param("delta") BigDecimal delta);
}
