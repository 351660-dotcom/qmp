package com.qmp.marketing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 核销优惠券请求（POST /api/v1/coupons/{couponId}/redeem）。
 */
@Data
public class RedeemCouponRequest {

    @JsonProperty("order_id")
    private Long orderId;
}
