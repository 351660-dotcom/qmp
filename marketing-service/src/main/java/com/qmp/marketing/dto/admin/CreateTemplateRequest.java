package com.qmp.marketing.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台创建优惠券模板请求（POST /admin/v1/coupon-templates）。
 */
@Data
public class CreateTemplateRequest {

    private String name;

    /** FULL_REDUCTION/DISCOUNT/EXCHANGE。 */
    @JsonProperty("coupon_type")
    private String couponType;

    @JsonProperty("face_value")
    private BigDecimal faceValue;

    @JsonProperty("discount_rate")
    private BigDecimal discountRate;

    @JsonProperty("applicable_scope")
    private JsonNode applicableScope;

    @JsonProperty("valid_period_rule")
    private JsonNode validPeriodRule;

    @JsonProperty("issue_quota")
    private Integer issueQuota;
}
