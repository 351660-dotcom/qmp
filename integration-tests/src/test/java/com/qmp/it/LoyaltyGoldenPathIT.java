package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会员与营销黄金路径：
 * - 积分：下门票单 → 支付 → 订单 PAID 发 OrderPaid → member 按金额自动入账积分；
 * - 优惠券：后台建模板 → 发券 → 核销 → 回退。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("会员营销黄金路径：消费自动积分 + 优惠券发放/核销")
class LoyaltyGoldenPathIT extends E2eSupport {

    @Test
    void pointsEarnedOnOrderPaid() {
        awaitServiceUp(ORDER_BASE + "/api/v1/orders/0");
        long userId = 123L; // 黄金路径会员（种子），享会员价 88

        int before = pointBalance(userId);

        // 下门票单（1 张，会员价 88）→ 支付 → 回调
        JsonNode created = post(ORDER_BASE + "/api/v1/orders", """
                {"user_id":123,"items":[{"sku_id":1001,"sale_date":"2026-07-01","time_slot_id":1,"quantity":1}]}
                """);
        long orderId = created.get("order_id").asLong();
        assertThat(created.get("total_amount").asDouble()).isEqualTo(88.00);

        JsonNode pay = post(ORDER_BASE + "/api/v1/orders/" + orderId + "/pay", "{\"channel\":\"WECHAT\"}");
        post(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"LOYALTY-IT-1\"}"
                        .formatted(pay.get("payment_id").asText()));

        awaitUntil(Duration.ofSeconds(60), () ->
                "PAID".equals(get(ORDER_BASE + "/api/v1/orders/" + orderId).get("status").asText()));

        // 积分按金额入账（1 元=1 分），异步
        awaitUntil(Duration.ofSeconds(60), () -> pointBalance(userId) >= before + 88);
    }

    @Test
    void couponIssueRedeemRevert() {
        awaitServiceUp(MARKETING_BASE + "/api/v1/coupons?user_id=0");
        long userId = uniqueId();

        JsonNode tpl = post(MARKETING_BASE + "/admin/v1/coupon-templates", """
                {"name":"满100减20","coupon_type":"FULL_REDUCTION","face_value":20.00,"issue_quota":100}
                """);
        long templateId = tpl.get("template_id").asLong();

        JsonNode coupon = post(MARKETING_BASE + "/api/v1/coupons/issue", """
                {"template_id":%d,"user_id":%d}
                """.formatted(templateId, userId));
        long couponId = coupon.get("coupon_id").asLong();
        assertThat(coupon.get("status").asText()).isEqualTo("UNUSED");

        JsonNode redeemed = post(MARKETING_BASE + "/api/v1/coupons/" + couponId + "/redeem",
                "{\"order_id\":987654321}");
        assertThat(redeemed.get("status").asText()).isEqualTo("USED");

        JsonNode reverted = post(MARKETING_BASE + "/api/v1/coupons/" + couponId + "/revert", null);
        assertThat(reverted.get("status").asText()).isEqualTo("UNUSED");
    }

    private int pointBalance(long userId) {
        return get(MEMBER_BASE + "/api/v1/members/" + userId + "/points").get("balance").asInt();
    }
}
