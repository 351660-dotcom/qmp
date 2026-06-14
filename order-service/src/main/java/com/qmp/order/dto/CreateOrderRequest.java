package com.qmp.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建订单请求（09 文档八）。
 */
@Data
public class CreateOrderRequest {

    @JsonProperty("user_id")
    private Long userId;

    private List<Item> items;

    @Data
    public static class Item {
        @JsonProperty("sku_id")
        private Long skuId;

        @JsonProperty("sale_date")
        private LocalDate saleDate;

        @JsonProperty("time_slot_id")
        private Long timeSlotId;

        private Integer quantity;
    }
}
