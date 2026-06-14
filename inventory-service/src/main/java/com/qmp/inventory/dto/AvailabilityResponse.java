package com.qmp.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * GET /api/v1/inventory/availability 响应（见 09 文档五）。
 */
@Getter
@Builder
public class AvailabilityResponse {

    @JsonProperty("total_quota")
    private int totalQuota;

    @JsonProperty("sold_count")
    private int soldCount;

    @JsonProperty("locked_count")
    private int lockedCount;

    private int remain;
}
