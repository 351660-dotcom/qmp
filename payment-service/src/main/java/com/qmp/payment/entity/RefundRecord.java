package com.qmp.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录（10 文档 8.3 {@code refund_record}）。一张票仅一条有效退款记录（{@code uk_credential}）。
 */
@Data
@TableName("refund_record")
public class RefundRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long refundId;

    private Long tenantId;

    private String paymentId;

    private Long credentialId;

    private BigDecimal amount;

    /** PENDING/SUCCEEDED/FAILED */
    private String status;

    private String refundChannelNo;

    @TableField(value = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
