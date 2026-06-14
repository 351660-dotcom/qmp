package com.qmp.verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出票请求（09 文档七 {@code POST /api/v1/credentials}）。
 *
 * <p>v1 在文档示例字段（order_item_id/sku_id/sale_date/quantity/refund_policy_snapshot）基础上，
 * 扩展了 order_id/scenic_id/unit_price/payment_id —— 这些值在 order-service 消费
 * {@code PaymentSucceeded} 出票时均已掌握，冗余下发可让本服务自给自足完成核验/退票，
 * 无需在核验或退票时再同步回查其他服务（不破坏 ADR-005 模块边界）。见 CLAUDE.md。</p>
 */
@Data
public class IssueCredentialsRequest {

    /** 幂等键。约定 = inventory 的 reservation_id。 */
    @JsonProperty("order_item_id")
    private String orderItemId;

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("sale_date")
    private LocalDate saleDate;

    private Integer quantity;

    /** 退改规则快照，原文透传存储（{type, cutoff_hours, refund_ratio}）。 */
    @JsonProperty("refund_policy_snapshot")
    private JsonNode refundPolicySnapshot;

    // ---- v1 扩展字段 ----

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("unit_price")
    private BigDecimal unitPrice;

    @JsonProperty("payment_id")
    private String paymentId;
}
