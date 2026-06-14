package com.qmp.hotel.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 后台创建/调整房晚库存请求（POST /admin/v1/room-buckets）。
 * 支持按区间 [from_date, to_date) 批量铺库存；按 (sku_id, sale_date) 幂等。
 */
@Data
public class UpsertRoomBucketRequest {

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("from_date")
    private LocalDate fromDate;

    /** 不含；缺省为 from_date 的次日（仅铺一晚）。 */
    @JsonProperty("to_date")
    private LocalDate toDate;

    @JsonProperty("total_quota")
    private Integer totalQuota;
}
