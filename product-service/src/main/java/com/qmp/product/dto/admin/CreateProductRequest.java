package com.qmp.product.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 后台创建门票商品请求（POST /admin/v1/products）。
 */
@Data
public class CreateProductRequest {

    private String name;

    private String description;

    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    /** JSON：有效期规则，如 {type:FIXED_DATE, valid_days:1}。 */
    @JsonProperty("valid_period_rule")
    private JsonNode validPeriodRule;

    /** NONE/ONE_TICKET_ONE_ID/ONE_ORDER_MULTI_PERSON。 */
    @JsonProperty("real_name_rule")
    private String realNameRule;

    /** 可选，退改签规则 JSON，如 {"type":"TIERED","cutoff_hours":24,"refund_ratio":0.8}；NONE=不可退。 */
    @JsonProperty("refund_policy")
    private JsonNode refundPolicy;

    /** 可选，缺省 DRAFT。DRAFT/PENDING_REVIEW/ON_SALE/OFF_SALE。 */
    private String status;
}
