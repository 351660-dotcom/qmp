package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 退改签规则按产品配置端到端：后台建带自定义退改规则（refund_ratio=0.5）的产品+票种 → 下单 →
 * order_item.refund_policy_snapshot 取该产品的真实快照（替代硬编码默认 0.8）。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("退改签规则按产品配置：下单快照取产品退改规则")
class RefundPolicyFromProductIT extends E2eSupport {

    private static final String PRICING_BASE = env("PRICING_BASE_URL", "http://localhost:8082");

    @Test
    void orderSnapshotsProductRefundPolicy() {
        awaitServiceUp(ORDER_BASE + "/api/v1/orders/0");

        // 1) 后台建产品（自定义退改规则 0.5）+ 票种（ON_SALE）
        JsonNode product = post(PRODUCT_BASE + "/admin/v1/products", """
                {"name":"退改测试产品","scenic_id":%d,"merchant_id":%d,
                 "valid_period_rule":{"type":"FIXED_DATE","valid_days":1},
                 "real_name_rule":"NONE",
                 "refund_policy":{"type":"TIERED","cutoff_hours":24,"refund_ratio":0.5},
                 "status":"ON_SALE"}
                """.formatted(SCENIC_ID, MERCHANT_ID));
        long productId = product.get("product_id").asLong();
        JsonNode sku = post(PRODUCT_BASE + "/admin/v1/skus",
                "{\"product_id\":%d,\"ticket_type\":\"ADULT\",\"requires_time_slot\":false}".formatted(productId));
        long skuId = sku.get("sku_id").asLong();

        // 2) 铺价格 + 库存（无场次 → time_slot_id=0）
        String saleDate = LocalDate.now().plusDays(58).toString();
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"MEMBER\",\"price\":80.00}".formatted(skuId, saleDate));
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"RETAIL\",\"price\":100.00}".formatted(skuId, saleDate));
        post(INVENTORY_BASE + "/admin/v1/buckets",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"time_slot_id\":0,\"scenic_id\":%d,\"total_quota\":100}"
                        .formatted(skuId, saleDate, SCENIC_ID));

        // 3) 下单
        JsonNode created = post(ORDER_BASE + "/api/v1/orders", """
                {"user_id":123,"items":[{"sku_id":%d,"sale_date":"%s","quantity":1}]}
                """.formatted(skuId, saleDate));
        long orderId = created.get("order_id").asLong();
        assertThat(created.get("status").asText()).isEqualTo("PENDING_PAYMENT");

        // 4) 明细退改快照取自产品配置（含 refund_ratio 0.5，非默认 0.8）
        String snapshot = scalar("SELECT refund_policy_snapshot FROM order_db.order_item WHERE order_id = " + orderId);
        assertThat(snapshot).contains("refund_ratio").contains("0.5");
        assertThat(snapshot).doesNotContain("0.8");
    }
}
