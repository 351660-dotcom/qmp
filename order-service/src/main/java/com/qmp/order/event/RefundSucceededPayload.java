package com.qmp.order.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 消费侧 {@code RefundSucceeded} payload（topic={@code payment_refund-succeeded}，payment-service 发布）。
 * order-service 据 {@code order_id} 累加 {@code refund_amount}（凭证终态/库存释放由 ticket-verification 处理）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundSucceededPayload {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("refund_id")
    private Long refundId;

    private BigDecimal amount;
}
