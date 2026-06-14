package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 演出/游船/游乐黄金路径：后台建场次+铺场次库存 → 场次预订 → 支付回调 → 确认；外加手牌二次消费。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("演出黄金路径：建场次/铺库存→预订→支付→确认 + 手牌消费")
class PerformanceGoldenPathIT extends E2eSupport {

    @Test
    void sessionBookingAndWristband() {
        awaitServiceUp(PERFORMANCE_BASE + "/api/v1/performance/bookings/0");

        long skuId = uniqueId();

        // 1) 后台建场次（ON_SALE）+ 铺场次库存
        JsonNode session = post(PERFORMANCE_BASE + "/admin/v1/sessions", """
                {"scenic_id":%d,"merchant_id":%d,"sku_id":%d,"name":"夜游船班次","session_type":"BOAT",
                 "start_time":"2026-08-01T19:30:00","base_price":150.00,"status":"ON_SALE"}
                """.formatted(SCENIC_ID, MERCHANT_ID, skuId));
        long sessionId = session.get("session_id").asLong();
        post(PERFORMANCE_BASE + "/admin/v1/session-buckets", """
                {"session_id":%d,"scenic_id":%d,"total_quota":100}
                """.formatted(sessionId, SCENIC_ID));

        // 2) 场次预订（2 位）
        JsonNode booking = post(PERFORMANCE_BASE + "/api/v1/performance/bookings/session", """
                {"user_id":123,"session_id":%d,"quantity":2}
                """.formatted(sessionId));
        long bookingId = booking.get("booking_id").asLong();
        assertThat(booking.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(booking.get("total_amount").asDouble()).isEqualTo(300.00);

        // 3) 支付 + 回调
        JsonNode pay = post(PERFORMANCE_BASE + "/api/v1/performance/bookings/" + bookingId + "/pay",
                "{\"channel\":\"WECHAT\"}");
        String paymentId = pay.get("payment_id").asText();
        post(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"PERF-IT-1\"}".formatted(paymentId));

        // 4) 等待确认：预订 CONFIRMED + 场次桶售出 2
        awaitUntil(Duration.ofSeconds(60), () ->
                "CONFIRMED".equals(get(PERFORMANCE_BASE + "/api/v1/performance/bookings/" + bookingId)
                        .get("status").asText()));
        String sold = scalar("SELECT sold_count FROM performance_db.session_inventory_bucket "
                + "WHERE session_id = " + sessionId);
        assertThat(Integer.parseInt(sold)).isEqualTo(2);

        // 5) 手牌二次消费：办理（初始 100）→ 消费 30 → 余额 70
        JsonNode band = post(PERFORMANCE_BASE + "/api/v1/performance/wristbands", """
                {"scenic_id":%d,"user_id":123,"initial_amount":100.00}
                """.formatted(SCENIC_ID));
        long wristbandId = band.get("wristband_id").asLong();
        assertThat(band.get("balance").asDouble()).isEqualTo(100.00);

        // source_ref 用本轮唯一的 wristbandId，避免重复运行命中全局幂等键 (source_ref, type) 而短路扣减。
        JsonNode consume = post(PERFORMANCE_BASE + "/api/v1/performance/wristbands/" + wristbandId + "/consume",
                """
                {"amount":30.00,"merchant_id":%d,"source_ref":"PERF-WB-IT-%d"}
                """.formatted(MERCHANT_ID, wristbandId));
        assertThat(consume.get("balance").asDouble()).isEqualTo(70.00);
    }
}
