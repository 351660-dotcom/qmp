package com.qmp.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 储值消费请求（12 文档 DeductWallet：dining-pos 结账「会员储值支付」调用）。
 */
@Data
public class DeductWalletRequest {

    private BigDecimal amount;

    @JsonProperty("merchant_id")
    private Long merchantId;

    /** 幂等键（如台账/订单号）。 */
    @JsonProperty("source_ref")
    private String sourceRef;
}
