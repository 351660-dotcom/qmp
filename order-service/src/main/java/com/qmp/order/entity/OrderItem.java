package com.qmp.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单明细（10 文档 6.2 {@code order_item}）。{@code order_item_id} 同时作为
 * inventory 的 {@code reservation_id} 与 ticket-verification 的 {@code order_item_id}。
 *
 * <p>v1 偏离：{@code order_item_id} 用 {@code VARCHAR(64)}（形如 {@code OI-...}）而非 10 文档的 BIGINT，
 * 以与 inventory/ticket-verification 的字符串约定一致。新增 {@code verified_count} 用于凭证核销聚合。
 * 见 CLAUDE.md。</p>
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.INPUT)
    private String orderItemId;

    private Long tenantId;

    private Long orderId;

    private Long skuId;

    private LocalDate saleDate;

    private Long timeSlotId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

    /** 退改规则快照 JSON，原文存储，出票时透传给 ticket-verification。 */
    private String refundPolicySnapshot;

    /** v1 扩展：已核销凭证数，消费 TicketVerified 时自增；= quantity 视为该明细全部核销。 */
    private Integer verifiedCount;

    @TableField(value = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
