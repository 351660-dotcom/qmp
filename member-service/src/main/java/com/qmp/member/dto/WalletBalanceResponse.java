package com.qmp.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 储值余额响应。
 */
@Getter
@Builder
public class WalletBalanceResponse {

    @JsonProperty("user_id")
    private Long userId;

    private BigDecimal balance;
}
