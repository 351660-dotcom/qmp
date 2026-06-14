package com.qmp.hotel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 预订单发起支付响应（透传 payment-service 的 pay_params）。
 */
@Getter
@Builder
public class PayResponse {

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("pay_params")
    private Map<String, Object> payParams;
}
