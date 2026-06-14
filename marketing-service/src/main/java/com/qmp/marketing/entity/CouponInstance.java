package com.qmp.marketing.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券实例（13 文档 4.2）。状态机：UNUSED -> USED（核销）/ EXPIRED；取消回退 USED -> UNUSED。
 */
@Data
@TableName("coupon_instance")
public class CouponInstance {

    @TableId(type = IdType.ASSIGN_ID)
    private Long couponId;

    private Long tenantId;

    private Long templateId;

    private Long userId;

    /** UNUSED/USED/EXPIRED。 */
    private String status;

    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    /** 核销时关联订单。 */
    private Long orderId;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
