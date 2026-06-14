package com.qmp.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 票种 SKU（10 文档 2.2 {@code ticket_sku}）。
 */
@Data
@TableName("ticket_sku")
public class TicketSku {

    @TableId(type = IdType.ASSIGN_ID)
    private Long skuId;

    private Long tenantId;

    private Long productId;

    /** ADULT/CHILD/SENIOR/STUDENT/MILITARY */
    private String ticketType;

    private Boolean requiresTimeSlot;

    /** JSON 数组，如 ["09:00-11:00","11:00-13:00"]，无分时预约时为 null。 */
    private String timeSlotDefinitions;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
