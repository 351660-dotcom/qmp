package com.qmp.supplychain.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 消费侧 {@code DiningChecked} payload（topic={@code dining.dining-checked}，dining-pos 发布）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiningCheckedPayload {

    @JsonProperty("table_order_id")
    private Long tableOrderId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    private List<Line> lines;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Line {
        @JsonProperty("sku_id")
        private Long skuId;
        private Integer quantity;
    }
}
