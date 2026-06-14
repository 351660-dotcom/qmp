package com.qmp.dining.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 设置菜品沽清状态请求。
 */
@Data
public class DishStatusRequest {

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    /** AVAILABLE/SOLD_OUT。 */
    private String status;
}
