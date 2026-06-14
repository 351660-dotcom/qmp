package com.qmp.dining.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 台账（12 文档 1.3）。从开台到结账可多次加菜/退菜，独立建模（不复用门票 Order）。
 * 零售即时收银 table_id 为空（12 文档三）。
 */
@Data
@TableName("table_order")
public class TableOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long tableOrderId;

    private Long tenantId;

    private Long merchantId;

    private Long tableId;

    /** OPEN/SETTLING/CLOSED/VOIDED。 */
    private String status;

    private Integer guestCount;

    /** 关联会员（member_account.user_id），用于储值抵扣与积分。 */
    private Long memberId;

    private BigDecimal totalAmount;

    private BigDecimal walletPaidAmount;

    private BigDecimal payableAmount;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
