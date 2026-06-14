package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 营销深水区端到端：规则引擎试算（DISCOUNT→FULL_REDUCTION）+ 秒杀抢购（独立桶防超卖 + 限购1）。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("营销引擎黄金路径：满减折扣试算 + 秒杀抢购")
class MarketingEngineIT extends E2eSupport {

    @Test
    void promotionCalculate() {
        awaitServiceUp(MARKETING_BASE + "/api/v1/coupons?user_id=0");
        // 用独立租户隔离，避免历史规则干扰试算结果
        String tenant = String.valueOf(uniqueId());

        // 9 折 + 满 100 减 20
        rawPost(MARKETING_BASE + "/admin/v1/promotion-rules",
                "{\"rule_type\":\"DISCOUNT\",\"actions\":{\"discount_rate\":0.9}}", tenant);
        rawPost(MARKETING_BASE + "/admin/v1/promotion-rules",
                "{\"rule_type\":\"FULL_REDUCTION\",\"actions\":{\"threshold\":100,\"reduce\":20}}", tenant);

        // 原价 200 → 9 折 180 → 满减 20 → 160
        HttpResponse<String> resp = rawPost(MARKETING_BASE + "/api/v1/promotions/calculate",
                "{\"user_id\":123,\"items\":[{\"sku_id\":1,\"quantity\":1,\"unit_price\":200.00}]}", tenant);
        assertThat(resp.statusCode()).isEqualTo(200);
        JsonNode data = dataOf(resp);
        assertThat(data.get("original_amount").asDouble()).isEqualTo(200.00);
        assertThat(data.get("payable_amount").asDouble()).isEqualTo(160.00);
        assertThat(data.get("discount_amount").asDouble()).isEqualTo(40.00);
        assertThat(data.get("applied_rules")).hasSize(2);
    }

    @Test
    void seckillSnapAndSoldOut() {
        awaitServiceUp(MARKETING_BASE + "/api/v1/coupons?user_id=0");
        long skuId = uniqueId();

        // 建秒杀活动（ACTIVE，超宽时间窗规避容器/宿主时区差）+ 仅 1 个名额
        JsonNode act = post(MARKETING_BASE + "/admin/v1/seckill-activities", """
                {"sku_id":%d,"seckill_price":9.90,"start_time":"2020-01-01T00:00:00","end_time":"2099-01-01T00:00:00","status":"ACTIVE"}
                """.formatted(skuId));
        long activityId = act.get("activity_id").asLong();
        post(MARKETING_BASE + "/admin/v1/seckill-buckets", """
                {"activity_id":%d,"sku_id":%d,"total_quota":1}
                """.formatted(activityId, skuId));

        // 用户 A 抢到
        JsonNode snapA = post(MARKETING_BASE + "/api/v1/seckill/" + activityId + "/snap",
                "{\"user_id\":1001}");
        assertThat(snapA.get("status").asText()).isEqualTo("HOLDING");
        assertThat(snapA.get("seckill_price").asDouble()).isEqualTo(9.90);

        // 用户 A 重复抢 → 409（限购 1）
        assertThat(rawPost(MARKETING_BASE + "/api/v1/seckill/" + activityId + "/snap",
                "{\"user_id\":1001}", TENANT_ID).statusCode()).isEqualTo(409);

        // 用户 B 抢 → 409（名额已抢完）
        assertThat(rawPost(MARKETING_BASE + "/api/v1/seckill/" + activityId + "/snap",
                "{\"user_id\":1002}", TENANT_ID).statusCode()).isEqualTo(409);

        // 库存桶 locked_count = 1（防超卖）
        String locked = scalar("SELECT locked_count FROM marketing_db.seckill_inventory_bucket "
                + "WHERE activity_id = " + activityId);
        assertThat(Integer.parseInt(locked)).isEqualTo(1);
    }
}
