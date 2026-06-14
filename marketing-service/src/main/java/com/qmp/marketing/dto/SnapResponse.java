package com.qmp.marketing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 秒杀抢购响应。
 */
@Getter
@Builder
public class SnapResponse {

    @JsonProperty("reservation_id")
    private String reservationId;

    private String status;

    @JsonProperty("seckill_price")
    private BigDecimal seckillPrice;
}
