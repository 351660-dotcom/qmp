package com.qmp.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单（10 文档 6.1 {@code trade_order}，{@code order} 为 MySQL 保留字故改名）。
 */
@Data
@TableName("trade_order")
public class TradeOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long orderId;

    private Long tenantId;

    private Long scenicId;

    private Long merchantId;

    private Long userId;

    /** PENDING_PAYMENT/PAID/CANCELLED/CLOSED（07 文档订单状态机）。 */
    private String status;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private BigDecimal refundAmount;

    private LocalDateTime payExpireAt;

    private String paymentId;

    @Version
    private Integer version;

    @TableField(value = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
