package com.qmp.order.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * {@code OrderPaid} 事件 payload（topic={@code order_order-paid}）。
 * 订单转 PAID 后发布，供 member-service 积分入账、marketing-service 营销履约/分摊消费（13 文档 2.4/3.4）。
 */
@Getter
@Builder
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
