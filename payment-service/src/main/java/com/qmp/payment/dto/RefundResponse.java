package com.qmp.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 退款相关接口的统一响应（见 09 文档六）。
 */
@Getter
@Builder
public class RefundResponse {

    @JsonProperty("refund_id")
    private Long refundId;

    /** PENDING/SUCCEEDED/FAILED */
    private String status;
}
