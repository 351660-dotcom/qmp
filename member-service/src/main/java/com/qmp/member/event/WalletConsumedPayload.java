package com.qmp.member.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * {@code WalletConsumed} 事件 payload（topic={@code member.wallet-consumed}）。
 * 会员储值消费（如餐饮结账储值抵扣）后发布，供统一对账归集（④）。
 */
@Getter
@Builder
public class WalletConsumedPayload {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    private BigDecimal amount;

    @JsonProperty("source_ref")
    private String sourceRef;
}
