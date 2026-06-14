package com.qmp.verification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 消费侧的 {@code RefundSucceeded} 事件 payload（topic={@code payment_refund-succeeded}，
 * 由 payment-service 发布，见其 RefundSucceededPayload）。本服务据 {@code credential_id}
 * 定位凭证并联动释放库存。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundSucceededPayload {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("refund_id")
    private Long refundId;

    @JsonProperty("credential_id")
    private Long credentialId;

    private BigDecimal amount;
}
