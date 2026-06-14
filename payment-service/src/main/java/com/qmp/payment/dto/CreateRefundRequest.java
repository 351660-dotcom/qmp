package com.qmp.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * POST /api/v1/payments/{payment_id}/refunds 请求体（见 09 文档六）。
 */
@Getter
@Setter
public class CreateRefundRequest {

    @JsonProperty("credential_id")
    private Long credentialId;

    private BigDecimal amount;
}
