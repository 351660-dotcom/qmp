package com.qmp.verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 申请退票响应（09 文档七）：{@code { refund_id }}（内部已调用 payment-service 创建退款记录）。
 */
@Getter
@Builder
public class RefundRequestResponse {

    @JsonProperty("refund_id")
    private Long refundId;
}
