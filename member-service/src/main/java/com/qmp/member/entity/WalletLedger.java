package com.qmp.member.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储值账本（13 文档 1.4）。{@code (source_ref, type)} 唯一作为幂等键。
 */
@Data
@TableName("wallet_ledger")
public class WalletLedger {

    @TableId(type = IdType.ASSIGN_ID)
    private Long ledgerId;

    private Long tenantId;

    private Long userId;

    private BigDecimal changeAmount;

    private BigDecimal balanceAfter;

    /** RECHARGE/CONSUME/REFUND/ADJUST。 */
    private String type;

    private Long merchantId;

    private String sourceRef;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
