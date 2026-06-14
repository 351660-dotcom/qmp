package com.qmp.reconciliation.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundSucceededPayload {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    @JsonProperty("refund_id")
    private Long refundId;

    private BigDecimal amount;
}
