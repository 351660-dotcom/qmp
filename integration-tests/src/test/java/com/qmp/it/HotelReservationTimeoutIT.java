package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 酒店预订超时关单端到端：连住预订（PENDING_PAYMENT，各晚 HOLDING）→ 构造创建时间已超 hold 窗口
 * → HotelExpireReservationJob 扫描 → 预订 CANCELLED + 各晚预占 RELEASED。
 *
 * <p>不真实等待 hold 窗口：用例直接将 {@code created_at} 改到过去，触发关单任务
 * （docker-compose 已把扫描间隔降到 5s）。</p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("酒店预订超时关单：未支付超时 → 释放各晚预占 + CANCELLED")
class HotelReservationTimeoutIT extends E2eSupport {

    @Test
    void expiredReservationIsCancelledAndReleased() {
        awaitServiceUp(HOTEL_BASE + "/api/v1/hotel/reservations/0");

        long skuId = uniqueId();
        LocalDate checkIn = LocalDate.now().plusDays(60);
        LocalDate checkOut = checkIn.plusDays(2); // 2 晚

        post(HOTEL_BASE + "/admin/v1/room-types", """
                {"name":"超时测试房","scenic_id":%d,"merchant_id":%d,"sku_id":%d,"base_price":300.00,"status":"ON_SALE"}
                """.formatted(SCENIC_ID, MERCHANT_ID, skuId));
        post(HOTEL_BASE + "/admin/v1/room-buckets", """
                {"sku_id":%d,"scenic_id":%d,"from_date":"%s","to_date":"%s","total_quota":5}
                """.formatted(skuId, SCENIC_ID, checkIn, checkOut));

        // 1) 连住预订：2 晚 HOLDING
        JsonNode created = post(HOTEL_BASE + "/api/v1/hotel/reservations", """
                {"user_id":123,"sku_id":%d,"check_in_date":"%s","check_out_date":"%s","room_count":1}
                """.formatted(skuId, checkIn, checkOut));
        long reservationId = created.get("reservation_id").asLong();
        assertThat(created.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(nightCount(reservationId, "HOLDING")).isEqualTo(2);

        // 2) 构造创建时间已超 hold 窗口（不等真实 30 分钟）
        int rows = execute("UPDATE hotel_db.room_reservation SET created_at = '2000-01-01 00:00:00' "
                + "WHERE reservation_id = " + reservationId);
        assertThat(rows).isEqualTo(1);

        // 3) 等待关单任务（间隔 5s）：预订 CANCELLED + 各晚预占 RELEASED
        awaitUntil(Duration.ofSeconds(60), () ->
                "CANCELLED".equals(reservationStatus(reservationId)) && nightCount(reservationId, "RELEASED") == 2);

        assertThat(reservationStatus(reservationId)).isEqualTo("CANCELLED");
        assertThat(nightCount(reservationId, "RELEASED")).isEqualTo(2);
        assertThat(nightCount(reservationId, "HOLDING")).isEqualTo(0);
    }

    private String reservationStatus(long reservationId) {
        return get(HOTEL_BASE + "/api/v1/hotel/reservations/" + reservationId).get("status").asText();
    }

    private int nightCount(long reservationId, String status) {
        String n = scalar("SELECT COUNT(*) FROM hotel_db.room_night_reservation "
                + "WHERE hotel_reservation_id = " + reservationId + " AND status = '" + status + "'");
        return Integer.parseInt(n);
    }
}
