package com.qmp.verification.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * {@code TicketVerified} 事件 payload（09 文档八.2，topic={@code ticket-verification.ticket-verified}）。
 * order-service 消费后更新对应 OrderItem 凭证聚合状态，若该订单所有凭证均为终态则 {@code Order.status -> CLOSED}。
 */
@Getter
@Builder
public class TicketVerifiedPayload {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("order_item_id")
    private String orderItemId;

    @JsonProperty("credential_id")
    private Long credentialId;

    @JsonProperty("verified_at")
    private Instant verifiedAt;
}
