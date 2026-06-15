package com.qmp.payment.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户分账抽成配置：每个商户可设不同的平台抽成比例（0..1）。
 * 由后台维护，{@code createSettlementRecord} 据此计算 platform/merchant 金额；未配置默认 0。
 */
@Data
@TableName("merchant_commission")
public class MerchantCommission {

    /** 商户ID（业务键，调用方传入）。 */
    @TableId(type = IdType.INPUT)
    private Long merchantId;

    private Long tenantId;

    /** 平台抽成比例 0..1。 */
    private BigDecimal commissionRate;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
