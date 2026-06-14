package com.qmp.dining.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 台账详情视图（含点单项）。
 */
@Getter
@Builder
public class TableOrderView {

    @JsonProperty("table_order_id")
    private Long tableOrderId;

    @JsonProperty("table_id")
    private Long tableId;

    private String status;

    @JsonProperty("member_id")
    private Long memberId;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    private List<Line> lines;

    @Getter
    @Builder
    public static class Line {
        @JsonProperty("order_line_id")
        private Long orderLineId;
        @JsonProperty("sku_id")
        private Long skuId;
        private Integer quantity;
        @JsonProperty("unit_price")
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private String status;
        @JsonProperty("requires_kitchen")
        private Boolean requiresKitchen;
        private String remark;
    }
}
