package com.qmp.marketing.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 后台创建营销规则请求（POST /admin/v1/promotion-rules）。
 */
@Data
public class CreatePromotionRuleRequest {

    /** MERCHANT/SCENIC/GROUP，缺省 SCENIC。 */
    private String scope;

    @JsonProperty("scope_id")
    private Long scopeId;

    /** FULL_REDUCTION/DISCOUNT。 */
    @JsonProperty("rule_type")
    private String ruleType;

    private JsonNode conditions;

    /** DISCOUNT:{discount_rate}; FULL_REDUCTION:{threshold,reduce}。 */
    private JsonNode actions;

    /** EXCLUSIVE/STACKABLE，缺省 EXCLUSIVE。 */
    @JsonProperty("stack_policy")
    private String stackPolicy;
}
