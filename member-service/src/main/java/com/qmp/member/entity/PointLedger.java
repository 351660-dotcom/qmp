package com.qmp.member.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分账本（13 文档 1.3，不可变追加）。{@code (source_ref, type)} 唯一作为幂等键。
 */
@Data
@TableName("point_ledger")
public class PointLedger {

    @TableId(type = IdType.ASSIGN_ID)
    private Long ledgerId;

    private Long tenantId;

    private Long userId;

    private Integer changeAmount;

    private Integer balanceAfter;

    /** EARN/REDEEM/EXPIRE/ADJUST。 */
    private String type;

    private Long sourceMerchantId;

    private String sourceRef;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
