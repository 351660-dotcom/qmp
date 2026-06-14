package com.qmp.hotel.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预订单（11 文档 2.1 Reservation 聚合根，v1 简化为单房型连住）。
 * {@code reservation_id} 为雪花数字 ID，同时作为 payment-service 的 {@code order_id}
 * （酒店与门票共用 payment 的 PaymentSucceeded 主题，各按 order_id 认领，见 CLAUDE.md）。
 */
@Data
@TableName("room_reservation")
public class RoomReservation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long reservationId;

    private Long tenantId;

    private Long scenicId;

    private Long merchantId;

    private Long userId;

    /** 房型 SKU。 */
    private Long skuId;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Integer nights;

    private Integer roomCount;

    /** PENDING_PAYMENT/CONFIRMED/CANCELLED（v1 子集，11 文档 2.4）。 */
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
