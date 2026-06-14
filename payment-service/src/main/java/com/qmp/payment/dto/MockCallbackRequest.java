package com.qmp.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * POST /internal/callbacks/mock 请求体（v1 模拟渠道回调，见本服务 CLAUDE.md）。
 */
@Getter
@Setter
public class MockCallbackRequest {

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("channel_trade_no")
    private String channelTradeNo;
}
