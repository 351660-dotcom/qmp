package com.qmp.supplychain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓库/中央厨房（12 文档 5.1）。
 */
@Data
@TableName("warehouse")
public class Warehouse {

    @TableId(type = IdType.ASSIGN_ID)
    private Long warehouseId;

    private Long tenantId;

    /** HQ/CENTRAL_KITCHEN/STORE。 */
    private String ownerScope;

    /** owner_scope=STORE 时关联餐饮商户。 */
    private Long merchantId;

    private String name;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
