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

    /** 退改签规则快照 JSON（支持 {"supported":bool,"cutoff_hours":24,"fee_ratio":0.2}；兼容旧 type/refund_ratio）。 */
    private String refundPolicy;

    /** 核销介质列表 JSON，如 ["QR_CODE","IC_CARD","FACE"]（核销规则之一，与 valid_period_rule 有效期并列）。 */
    private String verificationMedium;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
