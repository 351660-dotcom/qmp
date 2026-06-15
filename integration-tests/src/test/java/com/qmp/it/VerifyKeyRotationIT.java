package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 核销码按景区独立密钥 + 轮换端到端：景区 S 轮换出 v1 密钥 → 下单出票（用 v1 签名，码内嵌 kid=v1）
 * → 再轮换出 v2（v1 置 RETIRED 仍保留）→ 用 v1 签的旧码核验仍 SUCCESS（按 kid 取旧密钥）。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("核销码按景区密钥+轮换：轮换后旧码仍可离线验签")
class VerifyKeyRotationIT extends E2eSupport {

    private static final String PRICING_BASE = env("PRICING_BASE_URL", "http://localhost:8082");
    private static final String TICKET_BASE = env("TICKET_VERIFICATION_BASE_URL", "http://localhost:8086");

    @Test
    void oldCodeStillVerifiesAfterKeyRotation() {
        awaitServiceUp(ORDER_BASE + "/api/v1/orders/0");

        long scenicId = uniqueId(); // 独立景区，隔离密钥

        // 1) 景区 S 轮换出首版密钥 v1（ACTIVE）
        JsonNode k1 = post(TICKET_BASE + "/admin/v1/verify-keys/rotate?scenic_id=" + scenicId, null);
        assertThat(k1.get("key_version").asInt()).isEqualTo(1);

        // 2) 建该景区的产品/票种/价格/库存
        JsonNode product = post(PRODUCT_BASE + "/admin/v1/products", """
                {"name":"密钥轮换测试产品","scenic_id":%d,"merchant_id":%d,
                 "valid_period_rule":{"type":"FIXED_DATE","valid_days":1},"real_name_rule":"NONE","status":"ON_SALE"}
                """.formatted(scenicId, MERCHANT_ID));
        long productId = product.get("product_id").asLong();
        long skuId = post(PRODUCT_BASE + "/admin/v1/skus",
                "{\"product_id\":%d,\"ticket_type\":\"ADULT\",\"requires_time_slot\":false}".formatted(productId))
                .get("sku_id").asLong();
        String saleDate = LocalDate.now().plusDays(62).toString();
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"MEMBER\",\"price\":80.00}".formatted(skuId, saleDate));
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"RETAIL\",\"price\":100.00}".formatted(skuId, saleDate));
        post(INVENTORY_BASE + "/admin/v1/buckets",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"time_slot_id\":0,\"scenic_id\":%d,\"total_quota\":100}"
                        .formatted(skuId, saleDate, scenicId));

        // 3) 下单 + 支付 → 出票（用景区 S 的 v1 密钥签名）
        JsonNode created = post(ORDER_BASE + "/api/v1/orders", """
                {"user_id":123,"items":[{"sku_id":%d,"sale_date":"%s","quantity":1}]}
                """.formatted(skuId, saleDate));
        long orderId = created.get("order_id").asLong();
        String orderItemId = created.get("items").get(0).get("order_item_id").asText();
        JsonNode pay = post(ORDER_BASE + "/api/v1/orders/" + orderId + "/pay", "{\"channel\":\"WECHAT\"}");
        post(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"VKEY-IT-%d\"}".formatted(pay.get("payment_id").asText(), orderId));
        awaitUntil(Duration.ofSeconds(60), () -> verifyCode(orderItemId) != null);
        String oldCode = verifyCode(orderItemId);

        // 4) 再次轮换景区 S 密钥 → v2（v1 置 RETIRED 仍保留）
        JsonNode k2 = post(TICKET_BASE + "/admin/v1/verify-keys/rotate?scenic_id=" + scenicId, null);
        assertThat(k2.get("key_version").asInt()).isEqualTo(2);

        // 5) 用 v1 签的旧码核验仍成功（按 kid 取旧密钥离线验签）
        JsonNode verify = post(TICKET_BASE + "/api/v1/credentials/verify",
                "{\"verify_code\":\"%s\",\"terminal_id\":\"GATE-01\"}".formatted(oldCode));
        assertThat(verify.get("result").asText()).isEqualTo("SUCCESS");
    }

    private String verifyCode(String orderItemId) {
        return scalar("SELECT verify_code FROM ticket_verification_db.ticket_credential WHERE order_item_id = '"
                + orderItemId + "'");
    }
}
