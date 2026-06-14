package com.qmp.pricing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * GET /api/v1/price 响应（见 09 文档三）。
 */
@Getter
@Builder
public class PriceResponse {

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("sale_date")
    private LocalDate saleDate;

    @JsonProperty("price_type")
    private String priceType;

    private BigDecimal price;
}
