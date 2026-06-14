package com.qmp.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * POST /api/v1/payments 请求体（见 09 文档六）。
 */
@Getter
@Setter
public class CreatePaymentRequest {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("tenant_id")
    private Long tenantId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    private BigDecimal amount;

    /** WECHAT/ALIPAY */
    private String channel;
}
