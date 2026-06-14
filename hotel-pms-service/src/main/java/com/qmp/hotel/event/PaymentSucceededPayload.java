package com.qmp.hotel.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 消费侧 {@code PaymentSucceeded} payload（topic={@code payment.payment-succeeded}）。
 * {@code order_id} 即酒店预订单 reservation_id（门票订单与酒店预订单共用该主题，各按 id 认领）。
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
