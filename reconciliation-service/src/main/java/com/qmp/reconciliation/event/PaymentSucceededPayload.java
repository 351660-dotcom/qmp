package com.qmp.reconciliation.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentSucceededPayload {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    private BigDecimal amount;

    private String channel;

    @JsonProperty("paid_at")
    private Instant paidAt;
}
