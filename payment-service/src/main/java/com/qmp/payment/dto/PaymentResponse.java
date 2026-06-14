package com.qmp.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 支付单相关接口的统一响应（见 09 文档六）。
 */
@Getter
@Builder
public class PaymentResponse {

    @JsonProperty("payment_id")
    private String paymentId;

    /** CREATED/PAID/CLOSED */
    private String status;

    /** 透传给客户端调起支付SDK的参数，结构由 channel 决定；CREATED 时返回，CLOSE 等操作可为 null。 */
    @JsonProperty("pay_params")
    private Map<String, Object> payParams;
}
