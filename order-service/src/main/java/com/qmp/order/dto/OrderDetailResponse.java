package com.qmp.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 查询订单详情响应（09 文档八）。
 *
 * <p>v1 偏离：09 文档示例的 {@code items[].credentials[]} 为逐张凭证明细，但凭证归
 * ticket-verification-service 所有（ADR-005），order-service 不落库单张凭证，故以
 * {@code quantity}/{@code verified_count} 聚合表达核销进度（见 CLAUDE.md）。</p>
 */
@Getter
@Builder
public class OrderDetailResponse {

    @JsonProperty("order_id")
    private Long orderId;

    private String status;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("paid_amount")
    private BigDecimal paidAmount;

    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;

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

        @JsonProperty("verified_count")
        private Integer verifiedCount;
    }
}
