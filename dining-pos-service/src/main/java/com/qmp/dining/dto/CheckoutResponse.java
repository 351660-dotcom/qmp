package com.qmp.dining.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 结账响应。
 */
@Getter
@Builder
public class CheckoutResponse {

    @JsonProperty("table_order_id")
    private Long tableOrderId;

    private String status;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("wallet_paid_amount")
    private BigDecimal walletPaidAmount;

    /** 储值抵扣后仍需聚合支付的金额。 */
    @JsonProperty("payable_amount")
    private BigDecimal payableAmount;
}
