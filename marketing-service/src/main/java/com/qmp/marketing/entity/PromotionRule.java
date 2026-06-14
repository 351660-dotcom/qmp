package com.qmp.marketing.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 营销规则（13 文档 3.1）。v1 支持 FULL_REDUCTION/DISCOUNT。
 */
@Data
@TableName("promotion_rule")
public class PromotionRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long ruleId;

    private Long tenantId;

    /** MERCHANT/SCENIC/GROUP。 */
    private String scope;

    private Long scopeId;

    /** FULL_REDUCTION/DISCOUNT。 */
    private String ruleType;

    private String conditions;

    /** DISCOUNT:{discount_rate}; FULL_REDUCTION:{threshold,reduce}。 */
    private String actions;

    /** EXCLUSIVE/STACKABLE。 */
    private String stackPolicy;

    /** DRAFT/ACTIVE/EXPIRED。 */
    private String status;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
