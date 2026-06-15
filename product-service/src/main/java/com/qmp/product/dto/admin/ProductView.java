package com.qmp.product.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 后台商品列表视图。
 */
@Getter
@Builder
public class ProductView {

    @JsonProperty("product_id")
    private Long productId;

    private String name;

    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    private String status;

    @JsonProperty("real_name_rule")
    private String realNameRule;

    /** 退改签规则快照 JSON。 */
    @JsonProperty("refund_policy")
    private String refundPolicy;

    /** 核销介质列表 JSON，如 ["QR_CODE","IC_CARD","FACE"]。 */
    @JsonProperty("verification_medium")
    private String verificationMedium;
}
