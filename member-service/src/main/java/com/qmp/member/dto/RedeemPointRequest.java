package com.qmp.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 积分抵扣请求（13 文档 1.3 REDEEM）。
 */
@Data
public class RedeemPointRequest {

    private Integer points;

    @JsonProperty("merchant_id")
    private Long merchantId;

    /** 幂等键。 */
    @JsonProperty("source_ref")
    private String sourceRef;
}
