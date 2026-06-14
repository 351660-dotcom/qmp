package com.qmp.supplychain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品用料表（12 文档 6.1）。{@code materials} JSON：{@code [{material_sku_id, quantity, unit}]}。
 */
@Data
@TableName("dish_bom")
public class DishBom {

    @TableId(type = IdType.ASSIGN_ID)
    private Long bomId;

    private Long tenantId;

    /** 产出 SKU（菜品/半成品/成品）。 */
    private Long outputSkuId;

    private BigDecimal outputQuantity;

    /** 原料用量 JSON。 */
    private String materials;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
