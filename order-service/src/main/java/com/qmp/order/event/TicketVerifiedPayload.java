package com.qmp.order.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

/**
 * 消费侧 {@code TicketVerified} payload（topic={@code ticket-verification_ticket-verified}）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
