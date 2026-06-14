package com.qmp.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单（10 文档 8.1 {@code payment_order}）。一个订单仅一个有效支付单（{@code uk_order}）。
 */
@Data
@TableName("payment_order")
public class PaymentOrder {

    @TableId(type = IdType.INPUT)
    private String paymentId;

    private Long tenantId;

    private Long merchantId;

    private Long orderId;

    private BigDecimal amount;

    /** WECHAT/ALIPAY */
    private String channel;

    /** CREATED/PAID/CLOSED */
    private String status;

    private String channelTradeNo;

    private LocalDateTime paidAt;

    @TableField(value = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
