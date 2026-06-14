package com.qmp.marketing.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台创建秒杀活动请求（POST /admin/v1/seckill-activities）。
 */
@Data
public class CreateSeckillRequest {

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("seckill_price")
    private BigDecimal seckillPrice;

    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @JsonProperty("end_time")
    private LocalDateTime endTime;

    /** PENDING/ACTIVE/ENDED，缺省 ACTIVE（便于演示）。 */
    private String status;
}
