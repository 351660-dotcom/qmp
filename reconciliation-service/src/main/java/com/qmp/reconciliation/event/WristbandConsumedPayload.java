package com.qmp.reconciliation.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WristbandConsumedPayload {

    @JsonProperty("wristband_id")
    private Long wristbandId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    private BigDecimal amount;

    @JsonProperty("source_ref")
    private String sourceRef;
}
