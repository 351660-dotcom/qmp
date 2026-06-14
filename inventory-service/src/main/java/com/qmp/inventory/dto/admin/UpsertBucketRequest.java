package com.qmp.inventory.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDate;

/**
 * 后台创建/调整库存桶请求（POST /admin/v1/buckets）。
 * 按 (sku_id, sale_date, time_slot_id) 幂等：已存在则调整 total_quota，否则新建。
 */
@Data
public class UpsertBucketRequest {

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("sale_date")
    private LocalDate saleDate;

    /** 无分时预约固定为 0。 */
    @JsonProperty("time_slot_id")
    private Long timeSlotId;

    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("total_quota")
    private Integer totalQuota;

    /** 可选，渠道配额 JSON，缺省 {"direct": total_quota}。 */
    @JsonProperty("channel_quota")
    private JsonNode channelQuota;
}
