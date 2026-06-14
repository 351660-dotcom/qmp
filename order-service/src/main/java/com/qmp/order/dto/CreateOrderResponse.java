package com.qmp.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 创建订单响应（09 文档八）。
 */
@Getter
@Builder
public class CreateOrderResponse {

    @JsonProperty("order_id")
    private Long orderId;

    private String status;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("pay_expire_at")
    private Instant payExpireAt;

    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        @JsonProperty("order_item_id")
        private String orderItemId;

        @JsonProperty("sku_id")
        private Long skuId;

        private Integer quantity;

        @JsonProperty("unit_price")
        private BigDecimal unitPrice;
    }
}
