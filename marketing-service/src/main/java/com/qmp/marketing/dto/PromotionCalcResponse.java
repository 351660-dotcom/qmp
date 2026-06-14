package com.qmp.marketing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试算优惠响应。{@code applied_rules} 为命中规则快照，供订单留痕。
 */
@Getter
@Builder
public class PromotionCalcResponse {

    @JsonProperty("original_amount")
    private BigDecimal originalAmount;

    @JsonProperty("discount_amount")
    private BigDecimal discountAmount;

    @JsonProperty("payable_amount")
    private BigDecimal payableAmount;

    @JsonProperty("applied_rules")
    private List<Applied> appliedRules;

    @Getter
    @Builder
    public static class Applied {
        @JsonProperty("rule_id")
        private Long ruleId;
        @JsonProperty("rule_type")
        private String ruleType;
        @JsonProperty("discount_amount")
        private BigDecimal discountAmount;
    }
}
