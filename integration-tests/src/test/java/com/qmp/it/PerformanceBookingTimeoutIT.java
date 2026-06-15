package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 演出预订超时关单端到端：场次预订（PENDING_PAYMENT，预占 HOLDING）→ 构造创建时间已超 hold 窗口
 * → PerformanceExpireBookingJob 扫描 → 预订 CANCELLED + 场次预占 RELEASED。
 *
 * <p>不真实等待 hold 窗口：用例直接将 {@code created_at} 改到过去触发关单任务
 * （docker-compose 已把扫描间隔降到 5s）。</p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("演出预订超时关单：未支付超时 → 释放预占 + CANCELLED")
class PerformanceBookingTimeoutIT extends E2eSupport {

    private static final String PERFORMANCE_BASE = env("PERFORMANCE_BASE_URL", "http://localhost:8092");

    @Test
    void expiredBookingIsCancelledAndReleased() {
        awaitServiceUp(PERFORMANCE_BASE + "/api/v1/performance/bookings/0");

        long skuId = uniqueId();
        JsonNode session = post(PERFORMANCE_BASE + "/admin/v1/sessions", """
                {"scenic_id":%d,"merchant_id":%d,"sku_id":%d,"name":"超时测试场次","session_type":"RIDE",
                 "start_time":"2026-12-01T10:00:00","base_price":100.00,"status":"ON_SALE"}
                """.formatted(SCENIC_ID, MERCHANT_ID, skuId));
        long sessionId = session.get("session_id").asLong();
        post(PERFORMANCE_BASE + "/admin/v1/session-buckets",
                "{\"session_id\":%d,\"scenic_id\":%d,\"total_quota\":10}".formatted(sessionId, SCENIC_ID));

        // 1) 场次预订：预占 HOLDING
        JsonNode booking = post(PERFORMANCE_BASE + "/api/v1/performance/bookings/session",
                "{\"user_id\":123,\"session_id\":%d,\"quantity\":1}".formatted(sessionId));
        long bookingId = booking.get("booking_id").asLong();
        assertThat(booking.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(reservationCount(bookingId, "HOLDING")).isEqualTo(1);

        // 2) 构造创建时间已超 hold 窗口
        int rows = execute("UPDATE performance_db.performance_booking SET created_at = '2000-01-01 00:00:00' "
                + "WHERE booking_id = " + bookingId);
        assertThat(rows).isEqualTo(1);

        // 3) 等待关单任务（间隔 5s）：预订 CANCELLED + 预占 RELEASED
        awaitUntil(Duration.ofSeconds(60), () ->
                "CANCELLED".equals(bookingStatus(bookingId)) && reservationCount(bookingId, "RELEASED") == 1);

        assertThat(bookingStatus(bookingId)).isEqualTo("CANCELLED");
        assertThat(reservationCount(bookingId, "RELEASED")).isEqualTo(1);
        assertThat(reservationCount(bookingId, "HOLDING")).isEqualTo(0);
    }

    private String bookingStatus(long bookingId) {
        return get(PERFORMANCE_BASE + "/api/v1/performance/bookings/" + bookingId).get("status").asText();
    }

    private int reservationCount(long bookingId, String status) {
        String n = scalar("SELECT COUNT(*) FROM performance_db.performance_reservation "
                + "WHERE performance_booking_id = " + bookingId + " AND status = '" + status + "'");
        return Integer.parseInt(n);
    }
}
