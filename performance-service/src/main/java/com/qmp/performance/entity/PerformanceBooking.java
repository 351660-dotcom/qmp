package com.qmp.performance.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 演出/游船/游乐预订单（v1 自包含编排，类比 hotel）。{@code booking_id} 同时作为 payment 的 order_id。
 */
@Data
@TableName("performance_booking")
public class PerformanceBooking {

    @TableId(type = IdType.ASSIGN_ID)
    private Long bookingId;

    private Long tenantId;

    private Long scenicId;

    private Long merchantId;

    private Long userId;

    private Long sessionId;

    /** SESSION/SEAT。 */
    private String bucketRef;

    /** 选座场景的座位（多座以逗号分隔，v1 单座/单笔）。 */
    private String seatId;

    private Integer quantity;

    /** PENDING_PAYMENT/CONFIRMED/CANCELLED。 */
    private String status;

    private BigDecimal totalAmount;

    private String paymentId;

    @Version
    private Integer version;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
