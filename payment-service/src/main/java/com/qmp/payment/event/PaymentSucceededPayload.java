package com.qmp.payment.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code PaymentSucceeded} 事件 payload（见 09 文档八.2，topic={@code payment_payment-succeeded}）。
 */
@Getter
@Builder
public class PaymentSucceededPayload {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("payment_id")
    private String paymentId;

    /** 收款商户，供统一对账归集（④）。 */
    @JsonProperty("merchant_id")
    private Long merchantId;

    private BigDecimal amount;

    private String channel;

    @JsonProperty("paid_at")
    private Instant paidAt;
}
