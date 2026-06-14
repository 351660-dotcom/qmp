package com.qmp.marketing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 优惠券视图。
 */
@Getter
@Builder
public class CouponView {

    @JsonProperty("coupon_id")
    private Long couponId;

    @JsonProperty("template_id")
    private Long templateId;

    @JsonProperty("user_id")
    private Long userId;

    private String status;

    @JsonProperty("issued_at")
    private LocalDateTime issuedAt;

    @JsonProperty("used_at")
    private LocalDateTime usedAt;

    @JsonProperty("order_id")
    private Long orderId;
}
