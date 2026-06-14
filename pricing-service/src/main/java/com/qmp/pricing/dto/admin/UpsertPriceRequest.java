package com.qmp.pricing.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 后台设置价格请求（PUT /admin/v1/prices）。按 (sku_id, sale_date, price_type) 幂等 upsert。
 */
@Data
public class UpsertPriceRequest {

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("sale_date")
    private LocalDate saleDate;

    /** RETAIL（门市价）/ MEMBER（会员价）。 */
    @JsonProperty("price_type")
    private String priceType;

    private BigDecimal price;
}
