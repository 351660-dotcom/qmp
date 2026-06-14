package com.qmp.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 手牌账户视图。
 */
@Getter
@Builder
public class WristbandView {

    @JsonProperty("wristband_id")
    private Long wristbandId;

    @JsonProperty("user_id")
    private Long userId;

    private BigDecimal balance;

    private String status;
}
