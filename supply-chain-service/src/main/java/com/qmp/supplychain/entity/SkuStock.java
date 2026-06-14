package com.qmp.supplychain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存（12 文档 5.2）。出入库走条件更新（{@code quantity + delta >= 0}）防负，同 ADR-018 原则。
 *
 * <p>v1 偏离：用代理主键 {@code stock_id} + 唯一键 {@code (warehouse_id, sku_id)}，
 * 而非文档的复合主键（MyBatis-Plus 复合主键支持较弱），见 CLAUDE.md。</p>
 */
@Data
@TableName("sku_stock")
public class SkuStock {

    @TableId(type = IdType.ASSIGN_ID)
    private Long stockId;

    private Long tenantId;

    private Long warehouseId;

    private Long skuId;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal reorderPoint;

    @Version
    private Integer version;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
