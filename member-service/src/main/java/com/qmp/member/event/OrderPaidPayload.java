package com.qmp.member.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 消费侧 {@code OrderPaid} payload（topic={@code order_order-paid}，由 order-service 发布）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPaidPayload {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
}
