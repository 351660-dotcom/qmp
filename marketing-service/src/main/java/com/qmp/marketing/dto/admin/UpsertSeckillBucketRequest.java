package com.qmp.marketing.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 后台铺秒杀库存请求（POST /admin/v1/seckill-buckets）。按 activity_id 幂等。
 */
@Data
public class UpsertSeckillBucketRequest {

    @JsonProperty("activity_id")
    private Long activityId;

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("total_quota")
    private Integer totalQuota;
}
