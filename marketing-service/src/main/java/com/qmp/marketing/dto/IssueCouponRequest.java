package com.qmp.marketing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 发券请求（POST /api/v1/coupons/issue）。
 */
@Data
public class IssueCouponRequest {

    @JsonProperty("template_id")
    private Long templateId;

    @JsonProperty("user_id")
    private Long userId;
}
