package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分账抽成按商户配置端到端：后台为某商户设抽成 10% → 该商户一笔 200 元支付 →
 * settlement_record 平台分得 20.00、商户分得 180.00。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("分账抽成按商户配置：10% 抽成 → 平台 20 / 商户 180")
class CommissionSplitIT extends E2eSupport {

    @Test
    void settlementSplitsByMerchantCommissionRate() {
        awaitServiceUp(PAYMENT_BASE + "/actuator/health");

        long merchantId = uniqueId();
        long orderId = uniqueId();

        // 1) 后台设该商户抽成 10%
        put(PAYMENT_BASE + "/admin/v1/merchant-commissions",
                "{\"merchant_id\":%d,\"commission_rate\":0.10}".formatted(merchantId));

        // 2) 创建支付单（200 元）并模拟支付成功
        JsonNode pay = post(PAYMENT_BASE + "/api/v1/payments",
                "{\"order_id\":%d,\"tenant_id\":%s,\"merchant_id\":%d,\"amount\":200.00,\"channel\":\"WECHAT\"}"
                        .formatted(orderId, TENANT_ID, merchantId));
        String paymentId = pay.get("payment_id").asText();
        post(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"COMMISSION-IT-%d\"}".formatted(paymentId, orderId));

        // 3) 分账按商户比例：平台 20.00 / 商户 180.00
        String platform = scalar("SELECT platform_amount FROM payment_db.settlement_record WHERE payment_id = '"
                + paymentId + "'");
        String merchant = scalar("SELECT merchant_amount FROM payment_db.settlement_record WHERE payment_id = '"
                + paymentId + "'");
        assertThat(new java.math.BigDecimal(platform)).isEqualByComparingTo("20.00");
        assertThat(new java.math.BigDecimal(merchant)).isEqualByComparingTo("180.00");
    }
}
