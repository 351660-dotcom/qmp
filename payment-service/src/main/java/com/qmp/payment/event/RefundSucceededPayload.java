package com.qmp.payment.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * {@code RefundSucceeded} 事件 payload（见 09 文档八.2，topic={@code payment_refund-succeeded}）。
 */
@Getter
@Builder
public class RefundSucceededPayload {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("payment_id")
    private String paymentId;

    /** 退款商户，供统一对账归集（④）。 */
    @JsonProperty("merchant_id")
    private Long merchantId;

    @JsonProperty("refund_id")
    private Long refundId;

    @JsonProperty("credential_id")
    private Long credentialId;

    private BigDecimal amount;
}
