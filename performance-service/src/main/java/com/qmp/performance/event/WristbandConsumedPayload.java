package com.qmp.performance.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * {@code WristbandConsumed} 事件 payload（topic={@code performance.wristband-consumed}）。
 * 手牌二次消费后发布，供统一对账归集（④）。
 */
@Getter
@Builder
public class WristbandConsumedPayload {

    @JsonProperty("wristband_id")
    private Long wristbandId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    private BigDecimal amount;

    @JsonProperty("source_ref")
    private String sourceRef;
}
