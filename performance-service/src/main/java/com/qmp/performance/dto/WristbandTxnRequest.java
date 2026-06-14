package com.qmp.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 手牌充值/消费请求。消费时填 merchant_id。source_ref 为幂等键。
 */
@Data
public class WristbandTxnRequest {

    private BigDecimal amount;

    @JsonProperty("merchant_id")
    private Long merchantId;

    @JsonProperty("source_ref")
    private String sourceRef;
}
