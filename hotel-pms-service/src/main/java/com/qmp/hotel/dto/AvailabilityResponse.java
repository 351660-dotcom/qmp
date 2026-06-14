package com.qmp.hotel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 房晚可用量响应（按连住区间逐晚返回余量；min_remain 为整段最小余量，即可订间数上限）。
 */
@Getter
@Builder
public class AvailabilityResponse {

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("min_remain")
    private Integer minRemain;

    private List<Night> nights;

    @Getter
    @Builder
    public static class Night {
        @JsonProperty("sale_date")
        private LocalDate saleDate;
        private Integer remain;
    }
}
