package com.qmp.dining.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜品沽清（12 文档 2.2）。前厅运营状态，可人工标记。
 */
@Data
@TableName("dish_availability")
public class DishAvailability {

    @TableId(type = IdType.INPUT)
    private Long skuId;

    private Long tenantId;

    private Long merchantId;

    /** AVAILABLE/SOLD_OUT。 */
    private String status;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
