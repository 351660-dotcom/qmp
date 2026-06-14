package com.qmp.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 门票商品（10 文档 2.1 {@code ticket_product}）。
 */
@Data
@TableName("ticket_product")
public class TicketProduct {

    @TableId(type = IdType.ASSIGN_ID)
    private Long productId;

    private Long tenantId;

    private Long scenicId;

    private Long merchantId;

    private String name;

    private String description;

    /** DRAFT/PENDING_REVIEW/ON_SALE/OFF_SALE */
    private String status;

    /** JSON */
    private String validPeriodRule;

    /** NONE/ONE_TICKET_ONE_ID/ONE_ORDER_MULTI_PERSON */
    private String realNameRule;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
