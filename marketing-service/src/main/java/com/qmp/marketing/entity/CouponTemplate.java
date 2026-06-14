package com.qmp.marketing.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板（13 文档 4.1）。
 */
@Data
@TableName("coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long templateId;

    private Long tenantId;

    private String name;

    /** FULL_REDUCTION/DISCOUNT/EXCHANGE。 */
    private String couponType;

    /** 满减面额（FULL_REDUCTION）。 */
    private BigDecimal faceValue;

    /** 折扣率（DISCOUNT）。 */
    private BigDecimal discountRate;

    /** 适用范围 JSON（商品/商户）。 */
    private String applicableScope;

    /** 有效期规则 JSON：领取后 N 天 / 指定起止。 */
    private String validPeriodRule;

    /** 发放总量上限。 */
    private Integer issueQuota;

    /** ACTIVE/INACTIVE。 */
    private String status;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
