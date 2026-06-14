package com.qmp.payment.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code PaymentSucceeded} 事件 payload（见 09 文档八.2，topic={@code payment.payment-succeeded}）。
 */
@Getter
@Builder
public class PaymentSucceededPayload {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("payment_id")
    private String paymentId;

    private BigDecimal amount;

    private String channel;

    @JsonProperty("paid_at")
    private Instant paidAt;
}
