package com.qmp.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 储值充值请求（v1 直接入账；真实充值资金应走 payment 的储值专户，见 13 文档 1.4 与 CLAUDE.md）。
 */
@Data
public class RechargeRequest {

    private BigDecimal amount;

    /** 幂等键（如充值流水号）。 */
    @JsonProperty("source_ref")
    private String sourceRef;
}
