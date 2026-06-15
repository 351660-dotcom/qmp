package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 退改签规则之「手续费」端到端：产品退改规则 {supported:true, cutoff_hours:24, fee_ratio:0.25} →
 * 单票 80 元退票 → 退款额 = 80 × (1 − 0.25) = 60.00（手续费 25%）。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("退改签-手续费：fee_ratio 0.25 → 退款 60（单价 80）")
class RefundFeeRatioIT extends E2eSupport {

    private static final String PRICING_BASE = env("PRICING_BASE_URL", "http://localhost:8082");
    private static final String TICKET_BASE = env("TICKET_VERIFICATION_BASE_URL", "http://localhost:8086");

    @Test
    void refundAmountAppliesFeeRatio() {
        awaitServiceUp(ORDER_BASE + "/api/v1/orders/0");

        JsonNode product = post(PRODUCT_BASE + "/admin/v1/products", """
                {"name":"手续费测试产品","scenic_id":%d,"merchant_id":%d,
                 "valid_period_rule":{"type":"FIXED_DATE","valid_days":1},"real_name_rule":"NONE",
                 "refund_policy":{"supported":true,"cutoff_hours":24,"fee_ratio":0.25},"status":"ON_SALE"}
                """.formatted(SCENIC_ID, MERCHANT_ID));
        long productId = product.get("product_id").asLong();
        long skuId = post(PRODUCT_BASE + "/admin/v1/skus",
                "{\"product_id\":%d,\"ticket_type\":\"ADULT\",\"requires_time_slot\":false}".formatted(productId))
                .get("sku_id").asLong();
        String saleDate = LocalDate.now().plusDays(64).toString();
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"MEMBER\",\"price\":80.00}".formatted(skuId, saleDate));
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"RETAIL\",\"price\":100.00}".formatted(skuId, saleDate));
        post(INVENTORY_BASE + "/admin/v1/buckets",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"time_slot_id\":0,\"scenic_id\":%d,\"total_quota\":100}"
                        .formatted(skuId, saleDate, SCENIC_ID));

        JsonNode created = post(ORDER_BASE + "/api/v1/orders", """
                {"user_id":123,"items":[{"sku_id":%d,"sale_date":"%s","quantity":1}]}
                """.formatted(skuId, saleDate));
        long orderId = created.get("order_id").asLong();
        String orderItemId = created.get("items").get(0).get("order_item_id").asText();
        JsonNode pay = post(ORDER_BASE + "/api/v1/orders/" + orderId + "/pay", "{\"channel\":\"WECHAT\"}");
        post(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"FEE-IT-%d\"}".formatted(pay.get("payment_id").asText(), orderId));
        awaitUntil(Duration.ofSeconds(60), () -> credentialId(orderItemId) != null);

        // 退票 → 退款额按手续费 25% 计：80 × 0.75 = 60.00（经 order.refund_amount 验证）
        post(TICKET_BASE + "/api/v1/credentials/" + credentialId(orderItemId) + "/refund-request", null);
        awaitUntil(Duration.ofSeconds(60), () ->
                new java.math.BigDecimal("60.00").compareTo(orderRefundAmount(orderId)) == 0);
        assertThat(orderRefundAmount(orderId)).isEqualByComparingTo("60.00");
    }

    private Long credentialId(String orderItemId) {
        String s = scalar("SELECT credential_id FROM ticket_verification_db.ticket_credential WHERE order_item_id = '"
                + orderItemId + "'");
        return s == null ? null : Long.parseLong(s);
    }

    private java.math.BigDecimal orderRefundAmount(long orderId) {
        return get(ORDER_BASE + "/api/v1/orders/" + orderId).get("refund_amount").decimalValue();
    }
}
