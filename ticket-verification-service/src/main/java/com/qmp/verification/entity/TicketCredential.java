package com.qmp.verification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 核销凭证（10 文档 7.1 {@code ticket_credential} + v1 扩展字段，见本服务 CLAUDE.md）。
 *
 * <p>一个 {@code order_item}（数量 quantity）对应 quantity 条凭证（一票一证），
 * 支撑单张票的部分退改与离线核验。</p>
 */
@Data
@TableName("ticket_credential")
public class TicketCredential {

    @TableId(type = IdType.ASSIGN_ID)
    private Long credentialId;

    private Long tenantId;

    /** v1 扩展：订单 ID，用于 TicketVerified 事件分区 key（= order_id）与订单聚合定位。 */
    private Long orderId;

    /** 幂等键来源。约定 = inventory 的 reservation_id；v1 用 VARCHAR（见 CLAUDE.md 偏离说明）。 */
    private String orderItemId;

    /** 第几张票，1..quantity。 */
    private Integer ticketIndex;

    /** 核销凭证码（签名串，支持离线验签，见 07 文档 2.3）。 */
    private String verifyCode;

    /** UNUSED/VERIFIED/REFUNDING/REFUNDED/VOIDED/EXPIRED（07 文档 2.4 状态机）。 */
    private String status;

    /** v1 扩展：冗余景区 ID，写入 verification_record 免 join（10 文档设计决策 4）。 */
    private Long scenicId;

    /** v1 扩展：游玩日期，退票可退窗口计算依据。 */
    private LocalDate saleDate;

    /** v1 扩展：单票价，退款金额计算依据。 */
    private BigDecimal unitPrice;

    /** v1 扩展：所属支付单，退票时回调 payment-service 退款用。 */
    private String paymentId;

    /** v1 扩展：下单时快照的退改规则 JSON（{type, cutoff_hours, refund_ratio}），原文存储。 */
    private String refundPolicySnapshot;

    private LocalDateTime verifiedAt;

    private String verifyTerminalId;

    @TableField(value = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
