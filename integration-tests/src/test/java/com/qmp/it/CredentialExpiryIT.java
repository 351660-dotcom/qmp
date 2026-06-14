package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 凭证过期端到端：下单→支付→出票（UNUSED）→构造游玩日已过→ExpireCredentialJob 扫描→EXPIRED。
 *
 * <p>不真实等待游玩日：用例直接将凭证 {@code sale_date} 改到过去，触发过期任务
 * （docker-compose 已把扫描间隔降到 5s）。过期为 no-show 语义：不退款、不释放库存。</p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("凭证过期：游玩日已过仍未核销 → EXPIRED（no-show）")
class CredentialExpiryIT extends E2eSupport {

    private static final long SKU_ID = 1001L;
    private static final String PRICING_BASE = env("PRICING_BASE_URL", "http://localhost:8082");

    @Test
    void unusedCredentialExpiresAfterPlayDate() {
        awaitServiceUp(ORDER_BASE + "/api/v1/orders/0");

        String saleDate = LocalDate.now().plusDays(50).toString();
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"MEMBER\",\"price\":50.00}".formatted(SKU_ID, saleDate));
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"RETAIL\",\"price\":60.00}".formatted(SKU_ID, saleDate));
        post(INVENTORY_BASE + "/admin/v1/buckets",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"time_slot_id\":1,\"scenic_id\":%d,\"total_quota\":100}"
                        .formatted(SKU_ID, saleDate, SCENIC_ID));

        // 1) 下单 + 支付
        JsonNode created = post(ORDER_BASE + "/api/v1/orders", """
                {"user_id":123,"items":[{"sku_id":%d,"sale_date":"%s","time_slot_id":1,"quantity":1}]}
                """.formatted(SKU_ID, saleDate));
        long orderId = created.get("order_id").asLong();
        String orderItemId = created.get("items").get(0).get("order_item_id").asText();
        JsonNode pay = post(ORDER_BASE + "/api/v1/orders/" + orderId + "/pay", "{\"channel\":\"WECHAT\"}");
        post(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"EXPIRE-IT-%d\"}".formatted(pay.get("payment_id").asText(), orderId));

        // 2) 等待异步出票：凭证 UNUSED
        awaitUntil(Duration.ofSeconds(60), () -> "UNUSED".equals(credentialStatus(orderItemId)));
        Long credentialId = credentialId(orderItemId);
        assertThat(credentialId).isNotNull();

        // 3) 构造游玩日已过（不等真实游玩日）
        int rows = execute("UPDATE ticket_verification_db.ticket_credential "
                + "SET sale_date = '2000-01-01' WHERE credential_id = " + credentialId + " AND status = 'UNUSED'");
        assertThat(rows).isEqualTo(1);

        // 4) 等待过期任务（间隔 5s）：UNUSED -> EXPIRED
        awaitUntil(Duration.ofSeconds(60), () -> "EXPIRED".equals(credentialStatusById(credentialId)));
        assertThat(credentialStatusById(credentialId)).isEqualTo("EXPIRED");

        // 5) 过期为 no-show：库存预占保持 CONFIRMED（票款已确认收入，不回补）
        assertThat(reservationStatus(orderItemId)).isEqualTo("CONFIRMED");
    }

    private String credentialStatus(String orderItemId) {
        return scalar("SELECT status FROM ticket_verification_db.ticket_credential WHERE order_item_id = '"
                + orderItemId + "'");
    }

    private Long credentialId(String orderItemId) {
        String s = scalar("SELECT credential_id FROM ticket_verification_db.ticket_credential WHERE order_item_id = '"
                + orderItemId + "'");
        return s == null ? null : Long.parseLong(s);
    }

    private String credentialStatusById(long credentialId) {
        return scalar("SELECT status FROM ticket_verification_db.ticket_credential WHERE credential_id = "
                + credentialId);
    }

    private String reservationStatus(String orderItemId) {
        return scalar("SELECT status FROM inventory_db.inventory_reservation WHERE reservation_id = '"
                + orderItemId + "'");
    }
}
