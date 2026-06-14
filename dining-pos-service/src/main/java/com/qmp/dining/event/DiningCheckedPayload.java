package com.qmp.dining.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * {@code DiningChecked} 事件 payload（topic={@code dining_dining-checked}）。
 * 结账完成后发布，供 supply-chain-service 按 BOM 核减门店库存（12 文档 6.3）。
 */
@Getter
@Builder
public class DiningCheckedPayload {

    @JsonProperty("table_order_id")
    private Long tableOrderId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    private List<Line> lines;

    @Getter
    @Builder
    public static class Line {
        @JsonProperty("sku_id")
        private Long skuId;
        private Integer quantity;
    }
}
