package com.qmp.order.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 消费侧 {@code PaymentSucceeded} payload（topic={@code payment_payment-succeeded}）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
