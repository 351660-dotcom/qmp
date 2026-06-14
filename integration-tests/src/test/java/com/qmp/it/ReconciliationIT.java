package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨业态统一对账端到端：演出付款（PaymentSucceeded）+ 手牌消费（WristbandConsumed）
 * → reconciliation 汇成统一流水 → 日对账汇总按商户归集（PAYMENT + WRISTBAND）。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("统一对账黄金路径：多业态资金归集 + 日汇总")
class ReconciliationIT extends E2eSupport {

    @Test
    void crossBusinessReconciliation() {
        awaitServiceUp("http://localhost:8093/api/v1/reconciliation/daily?date=2026-01-01");

        long merchantId = uniqueId(); // 独立商户，隔离对账断言
        long skuId = uniqueId();
        LocalDate today = LocalDate.now();

        // 1) 演出：建场次（该商户）+ 库存 → 预订 → 支付回调（产生 PaymentSucceeded, IN/PAYMENT=100）
        JsonNode session = post(PERFORMANCE_BASE + "/admin/v1/sessions", """
                {"scenic_id":%d,"merchant_id":%d,"sku_id":%d,"name":"对账测试场次","session_type":"RIDE",
                 "start_time":"2026-09-01T10:00:00","base_price":100.00,"status":"ON_SALE"}
                """.formatted(SCENIC_ID, merchantId, skuId));
        long sessionId = session.get("session_id").asLong();
        post(PERFORMANCE_BASE + "/admin/v1/session-buckets",
                "{\"session_id\":%d,\"scenic_id\":%d,\"total_quota\":10}".formatted(sessionId, SCENIC_ID));
        JsonNode booking = post(PERFORMANCE_BASE + "/api/v1/performance/bookings/session",
                "{\"user_id\":123,\"session_id\":%d,\"quantity\":1}".formatted(sessionId));
        long bookingId = booking.get("booking_id").asLong();
        JsonNode pay = post(PERFORMANCE_BASE + "/api/v1/performance/bookings/" + bookingId + "/pay",
                "{\"channel\":\"WECHAT\"}");
        post(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"RECON-IT-1\"}".formatted(pay.get("payment_id").asText()));

        // 2) 手牌：办理（初始 50）→ 消费 30（该商户）→ 产生 WristbandConsumed, IN/WRISTBAND=30
        JsonNode band = post(PERFORMANCE_BASE + "/api/v1/performance/wristbands",
                "{\"scenic_id\":%d,\"user_id\":123,\"initial_amount\":50.00}".formatted(SCENIC_ID));
        long wristbandId = band.get("wristband_id").asLong();
        post(PERFORMANCE_BASE + "/api/v1/performance/wristbands/" + wristbandId + "/consume",
                "{\"amount\":30.00,\"merchant_id\":%d,\"source_ref\":\"RECON-WB-IT-1\"}".formatted(merchantId));

        // 3) 等待对账归集：日汇总 in_total=130，by_source 含 PAYMENT(100)+WRISTBAND(30)
        String reconBase = "http://localhost:8093/api/v1/reconciliation";
        awaitUntil(Duration.ofSeconds(60), () -> {
            JsonNode d = get(reconBase + "/daily?date=" + today + "&merchant_id=" + merchantId);
            return d.get("in_total").asDouble() == 130.0;
        });
        JsonNode summary = get(reconBase + "/daily?date=" + today + "&merchant_id=" + merchantId);
        assertThat(summary.get("out_total").asDouble()).isEqualTo(0.0);
        assertThat(summary.get("net").asDouble()).isEqualTo(130.0);
        assertThat(summary.get("by_source").get("PAYMENT").asDouble()).isEqualTo(100.0);
        assertThat(summary.get("by_source").get("WRISTBAND").asDouble()).isEqualTo(30.0);
    }
}
