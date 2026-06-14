package com.qmp.marketing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试算优惠请求（13 文档 3.2 GetApplicableRules）。order-service 创建订单时可调用并快照结果。
 */
@Data
public class PromotionCalcRequest {

    @JsonProperty("user_id")
    private Long userId;

    private List<Item> items;

    @Data
    public static class Item {
        @JsonProperty("sku_id")
        private Long skuId;
        private Integer quantity;
        @JsonProperty("unit_price")
        private BigDecimal unitPrice;
    }
}
