package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单超时关单端到端：创建订单（PENDING_PAYMENT，预占 HOLDING）→ 构造支付截止时间已过
 * → CancelExpiredOrderJob 扫描 → 订单置 CANCELLED + 预占释放（RELEASED）；过期订单不可再支付。
 *
 * <p>不真实等待 15 分钟支付窗口：用例直接将 {@code trade_order.pay_expire_at} 改到过去，
 * 触发关单任务（docker-compose 已把扫描间隔降到 5s）。游玩日期取动态未来日并经 admin 自助造数。</p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("订单超时关单：未支付超时 → 释放预占 + 置 CANCELLED + 拒绝再支付")
class OrderTimeoutCancelIT extends E2eSupport {

    private static final long SKU_ID = 1001L;
    private static final String PRICING_BASE = env("PRICING_BASE_URL", "http://localhost:8082");

    @Test
    void expiredOrderIsCancelledAndReleased() {
        awaitServiceUp(ORDER_BASE + "/api/v1/orders/0");

        // 独立未来日期 + 自助铺价格/库存，避免与门票黄金路径共用桶
        String saleDate = LocalDate.now().plusDays(45).toString();
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"MEMBER\",\"price\":50.00}".formatted(SKU_ID, saleDate));
        put(PRICING_BASE + "/admin/v1/prices",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"price_type\":\"RETAIL\",\"price\":60.00}".formatted(SKU_ID, saleDate));
        post(INVENTORY_BASE + "/admin/v1/buckets",
                "{\"sku_id\":%d,\"sale_date\":\"%s\",\"time_slot_id\":1,\"scenic_id\":%d,\"total_quota\":100}"
                        .formatted(SKU_ID, saleDate, SCENIC_ID));

        // 1) 创建订单：1 条明细，预占 HOLDING
        JsonNode created = post(ORDER_BASE + "/api/v1/orders", """
                {"user_id":123,"items":[{"sku_id":%d,"sale_date":"%s","time_slot_id":1,"quantity":1}]}
                """.formatted(SKU_ID, saleDate));
        long orderId = created.get("order_id").asLong();
        String orderItemId = created.get("items").get(0).get("order_item_id").asText();
        assertThat(created.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(reservationStatus(orderItemId)).isEqualTo("HOLDING");

        // 2) 构造支付截止时间已过（不等真实 15 分钟）
        int rows = execute("UPDATE order_db.trade_order SET pay_expire_at = '2000-01-01 00:00:00' WHERE order_id = "
                + orderId);
        assertThat(rows).isEqualTo(1);

        // 3) 等待关单任务（间隔 5s）：订单 CANCELLED + 预占 RELEASED
        awaitUntil(Duration.ofSeconds(60), () ->
                "CANCELLED".equals(orderStatus(orderId)) && "RELEASED".equals(reservationStatus(orderItemId)));

        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(reservationStatus(orderItemId)).isEqualTo("RELEASED");

        // 4) 已关闭订单不可再支付（409）
        HttpResponse<String> payResp = rawPost(ORDER_BASE + "/api/v1/orders/" + orderId + "/pay",
                "{\"channel\":\"WECHAT\"}", TENANT_ID);
        assertThat(payResp.statusCode()).as("pay closed order -> %s", payResp.body()).isEqualTo(409);
    }

    private String orderStatus(long orderId) {
        return get(ORDER_BASE + "/api/v1/orders/" + orderId).get("status").asText();
    }

    private String reservationStatus(String orderItemId) {
        return scalar("SELECT status FROM inventory_db.inventory_reservation WHERE reservation_id = '"
                + orderItemId + "'");
    }
}
