package com.qmp.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * GET /api/v1/skus/{sku_id} 响应（见 09 文档二）。
 */
@Getter
@Builder
public class SkuInfoResponse {

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("product_id")
    private Long productId;

    /** 来自 ticket_product，供 order-service 落 trade_order / 透传下游（v1 扩展，见 CLAUDE.md）。 */
    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    /** DRAFT/PENDING_REVIEW/ON_SALE/OFF_SALE，来自 ticket_product */
    private String status;

    @JsonProperty("ticket_type")
    private String ticketType;

    @JsonProperty("real_name_rule")
    private String realNameRule;

    /** 退改签规则快照（来自 ticket_product，供 order 下单落 order_item.refund_policy_snapshot）。 */
    @JsonProperty("refund_policy")
    private String refundPolicy;

    @JsonProperty("requires_time_slot")
    private Boolean requiresTimeSlot;

    @JsonProperty("time_slot_definitions")
    private List<String> timeSlotDefinitions;
}
