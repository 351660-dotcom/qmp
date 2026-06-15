package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 部分退票「按张释放」端到端：一笔明细买 2 张 → 退其中 1 张 → 只回补 1 个名额（预占仍 CONFIRMED、
 * released_quantity=1、桶 sold=1）；再退第 2 张 → 整笔 RELEASED、sold=0。
 *
 * <p>验证对原「整笔预占只能整笔释放」限制的修复：多张明细的单张退票不再超额回补库存。</p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("部分退票按张释放：2 张明细退 1 张只回补 1 个名额")
class CredentialPartialRefundIT extends E2eSupport {

    private static final long SKU_ID = 1001L;
    private static final String PRICING_BASE = env("PRICING_BASE_URL", "http://localhost:8082");
    private static final String TICKET_BASE = env("TICKET_VERIFICATION_BASE_URL", "http://localhost:8086");

    @Test
    void singleTicketRefundReleasesOneUnitOnly() {
        awaitServiceUp(ORDER_BASE + "/api/v1/orders/0");

        String saleDate = LocalDate.now().plusDays(55).toString();
        int slot = 1;
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"MEMBER\",\"price\":80.00}".formatted(SKU_ID, saleDate));
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"RETAIL\",\"price\":100.00}".formatted(SKU_ID, saleDate));
        post(INVENTORY_BASE + "/admin/v1/buckets",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"time_slot_id\":%d,\"scenic_id\":%d,\"total_quota\":100}"
                        .formatted(SKU_ID, saleDate, slot, SCENIC_ID));

        // 1) 下单：单明细 2 张
        JsonNode created = post(ORDER_BASE + "/api/v1/orders", """
                {"user_id":123,"items":[{"sku_id":%d,"sale_date":"%s","time_slot_id":%d,"quantity":2}]}
                """.formatted(SKU_ID, saleDate, slot));
        long orderId = created.get("order_id").asLong();
        String orderItemId = created.get("items").get(0).get("order_item_id").asText();

        // 2) 支付 → 出 2 张凭证、预占 CONFIRMED、桶 sold=2
        JsonNode pay = post(ORDER_BASE + "/api/v1/orders/" + orderId + "/pay", "{\"channel\":\"WECHAT\"}");
        post(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"PARTIAL-IT-%d\"}".formatted(pay.get("payment_id").asText(), orderId));
        awaitUntil(Duration.ofSeconds(60), () -> credentialCount(orderItemId, "UNUSED") == 2);
        assertThat(soldCount(saleDate, slot)).isEqualTo(2);

        long cred1 = credentialIdByIndex(orderItemId, 1);
        long cred2 = credentialIdByIndex(orderItemId, 2);

        // 3) 退第 1 张 → 只回补 1 个名额：桶 sold=1、预占 released_quantity=1、仍 CONFIRMED
        post(TICKET_BASE + "/api/v1/credentials/" + cred1 + "/refund-request", null);
        awaitUntil(Duration.ofSeconds(60), () ->
                "REFUNDED".equals(credentialStatusById(cred1)) && soldCount(saleDate, slot) == 1);
        assertThat(reservationReleasedQty(orderItemId)).isEqualTo(1);
        assertThat(reservationStatus(orderItemId)).isEqualTo("CONFIRMED"); // 仍有 1 张有效，未整笔释放
        assertThat(soldCount(saleDate, slot)).isEqualTo(1);

        // 4) 退第 2 张 → 整笔释放：桶 sold=0、released_quantity=2、RELEASED
        post(TICKET_BASE + "/api/v1/credentials/" + cred2 + "/refund-request", null);
        awaitUntil(Duration.ofSeconds(60), () ->
                "REFUNDED".equals(credentialStatusById(cred2)) && soldCount(saleDate, slot) == 0);
        assertThat(reservationReleasedQty(orderItemId)).isEqualTo(2);
        assertThat(reservationStatus(orderItemId)).isEqualTo("RELEASED");
        assertThat(soldCount(saleDate, slot)).isEqualTo(0);
    }

    private int soldCount(String saleDate, int slot) {
        JsonNode a = get(INVENTORY_BASE + "/api/v1/inventory/availability?sku_id=" + SKU_ID
                + "&sale_date=" + saleDate + "&time_slot_id=" + slot);
        return a.get("sold_count").asInt();
    }

    private int credentialCount(String orderItemId, String status) {
        String n = scalar("SELECT COUNT(*) FROM ticket_verification_db.ticket_credential "
                + "WHERE order_item_id = '" + orderItemId + "' AND status = '" + status + "'");
        return Integer.parseInt(n);
    }

    private long credentialIdByIndex(String orderItemId, int index) {
        return Long.parseLong(scalar("SELECT credential_id FROM ticket_verification_db.ticket_credential "
                + "WHERE order_item_id = '" + orderItemId + "' AND ticket_index = " + index));
    }

    private String credentialStatusById(long credentialId) {
        return scalar("SELECT status FROM ticket_verification_db.ticket_credential WHERE credential_id = " + credentialId);
    }

    private int reservationReleasedQty(String orderItemId) {
        return Integer.parseInt(scalar("SELECT released_quantity FROM inventory_db.inventory_reservation "
                + "WHERE reservation_id = '" + orderItemId + "'"));
    }

    private String reservationStatus(String orderItemId) {
        return scalar("SELECT status FROM inventory_db.inventory_reservation WHERE reservation_id = '" + orderItemId + "'");
    }
}
