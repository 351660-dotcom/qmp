package com.qmp.dining.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 桌台（12 文档 1.1）。
 */
@Data
@TableName("dining_table")
public class DiningTable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long tableId;

    private Long tenantId;

    private Long merchantId;

    private Long areaId;

    private String tableNo;

    private Integer capacity;

    /** IDLE/OCCUPIED/RESERVED/CLEANING。 */
    private String status;

    private Long currentTableOrderId;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
