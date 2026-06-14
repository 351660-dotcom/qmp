package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 酒店黄金路径：后台建房型+铺房晚库存 → 连住预订（多夜原子预占）→ 支付回调 → 确认预订。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("酒店黄金路径：建房型/铺房晚→连住预订→支付→确认")
class HotelGoldenPathIT extends E2eSupport {

    @Test
    void goldenPath() {
        awaitServiceUp(HOTEL_BASE + "/api/v1/hotel/reservations/0");

        long skuId = uniqueId();
        LocalDate checkIn = LocalDate.now().plusDays(40);
        LocalDate checkOut = checkIn.plusDays(2); // 2 晚

        // 1) 后台建房型（ON_SALE）+ 铺 2 晚房晚库存
        post(HOTEL_BASE + "/admin/v1/room-types", """
                {"name":"测试大床房","scenic_id":%d,"merchant_id":%d,"sku_id":%d,"base_price":300.00,"status":"ON_SALE"}
                """.formatted(SCENIC_ID, MERCHANT_ID, skuId));
        post(HOTEL_BASE + "/admin/v1/room-buckets", """
                {"sku_id":%d,"scenic_id":%d,"from_date":"%s","to_date":"%s","total_quota":5}
                """.formatted(skuId, SCENIC_ID, checkIn, checkOut));

        // 2) 可订量 = 整段最小余量
        JsonNode avail = get(HOTEL_BASE + "/api/v1/hotel/availability?sku_id=" + skuId
                + "&check_in_date=" + checkIn + "&check_out_date=" + checkOut);
        assertThat(avail.get("min_remain").asInt()).isEqualTo(5);

        // 3) 连住预订（2 晚，1 间）→ 多夜原子预占
        JsonNode created = post(HOTEL_BASE + "/api/v1/hotel/reservations", """
                {"user_id":123,"sku_id":%d,"check_in_date":"%s","check_out_date":"%s","room_count":1}
                """.formatted(skuId, checkIn, checkOut));
        long reservationId = created.get("reservation_id").asLong();
        assertThat(created.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(created.get("nights").asInt()).isEqualTo(2);
        assertThat(created.get("total_amount").asDouble()).isEqualTo(600.00);
        assertThat(nightCount(reservationId, "HOLDING")).isEqualTo(2);

        // 4) 支付 + 模拟回调
        JsonNode pay = post(HOTEL_BASE + "/api/v1/hotel/reservations/" + reservationId + "/pay",
                "{\"channel\":\"WECHAT\"}");
        String paymentId = pay.get("payment_id").asText();
        post(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"HOTEL-IT-1\"}".formatted(paymentId));

        // 5) 等待异步确认：预订单 CONFIRMED + 两晚预占 CONFIRMED
        awaitUntil(Duration.ofSeconds(60), () ->
                "CONFIRMED".equals(get(HOTEL_BASE + "/api/v1/hotel/reservations/" + reservationId)
                        .get("status").asText()));
        assertThat(nightCount(reservationId, "CONFIRMED")).isEqualTo(2);

        // 6) 库存账本：两晚各售出 1（余量 5→4）
        JsonNode availAfter = get(HOTEL_BASE + "/api/v1/hotel/availability?sku_id=" + skuId
                + "&check_in_date=" + checkIn + "&check_out_date=" + checkOut);
        assertThat(availAfter.get("min_remain").asInt()).isEqualTo(4);
    }

    private int nightCount(long reservationId, String status) {
        String n = scalar("SELECT COUNT(*) FROM hotel_db.room_night_reservation "
                + "WHERE hotel_reservation_id = " + reservationId + " AND status = '" + status + "'");
        return Integer.parseInt(n);
    }
}
