package com.qmp.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分账记录（10 文档 8.2 {@code settlement_record}）。v1 两级分账，一个支付单一条分账记录。
 */
@Data
@TableName("settlement_record")
public class SettlementRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long settlementId;

    private Long tenantId;

    private String paymentId;

    private Long merchantId;

    private BigDecimal platformAmount;

    private BigDecimal merchantAmount;

    /** PENDING/SETTLED/FAILED */
    private String status;

    private String settleChannelNo;

    @TableField(value = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
