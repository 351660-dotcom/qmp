package com.qmp.product.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 后台创建票种请求（POST /admin/v1/skus）。
 */
@Data
public class CreateSkuRequest {

    @JsonProperty("product_id")
    private Long productId;

    /** ADULT/CHILD/SENIOR/STUDENT/MILITARY。 */
    @JsonProperty("ticket_type")
    private String ticketType;

    @JsonProperty("requires_time_slot")
    private Boolean requiresTimeSlot;

    /** 分时场次定义，如 ["09:00-11:00","11:00-13:00"]；无分时为空。 */
    @JsonProperty("time_slot_definitions")
    private List<String> timeSlotDefinitions;
}
