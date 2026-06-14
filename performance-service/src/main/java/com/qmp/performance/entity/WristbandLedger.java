package com.qmp.performance.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 手牌账本（14 文档 4.2）。{@code (source_ref, type)} 唯一作为幂等键。
 */
@Data
@TableName("wristband_ledger")
public class WristbandLedger {

    @TableId(type = IdType.ASSIGN_ID)
    private Long ledgerId;

    private Long tenantId;

    private Long wristbandId;

    private BigDecimal changeAmount;

    private BigDecimal balanceAfter;

    /** RECHARGE/CONSUME/REFUND。 */
    private String type;

    private Long merchantId;

    private String sourceRef;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
