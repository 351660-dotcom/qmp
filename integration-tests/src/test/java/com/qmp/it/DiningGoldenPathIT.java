package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 餐饮黄金路径：后台建仓/铺料/配 BOM + 会员充值 + 建台 → 开台/点单/出品/结账（会员储值抵扣）
 * → 异步按 BOM 核减门店库存。串起 dining-pos + member（储值）+ supply-chain（BOM）。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("餐饮黄金路径：点单→出品→结账(储值抵扣)→BOM核减库存")
class DiningGoldenPathIT extends E2eSupport {

    @Test
    void diningCheckoutFlow() {
        awaitServiceUp(DINING_BASE + "/api/v1/dining/table-orders/0");

        long merchantId = uniqueId();
        long dishSku = uniqueId();
        long ingredientSku = uniqueId();
        long userId = uniqueId();

        // 1) 供应链：建门店仓 + 铺原料库存 1000g + 配 BOM（1 份菜耗 200g）
        JsonNode wh = post(SUPPLY_BASE + "/admin/v1/warehouses", """
                {"owner_scope":"STORE","merchant_id":%d,"name":"门店仓-IT"}
                """.formatted(merchantId));
        long warehouseId = wh.get("warehouse_id").asLong();
        post(SUPPLY_BASE + "/admin/v1/stocks", """
                {"warehouse_id":%d,"sku_id":%d,"quantity":1000,"unit":"g"}
                """.formatted(warehouseId, ingredientSku));
        post(SUPPLY_BASE + "/admin/v1/boms", """
                {"output_sku_id":%d,"output_quantity":1,"materials":[{"material_sku_id":%d,"quantity":200,"unit":"g"}]}
                """.formatted(dishSku, ingredientSku));

        // 2) 会员储值充值 1000
        JsonNode rc = post(MEMBER_BASE + "/api/v1/members/" + userId + "/wallet/recharge", """
                {"amount":1000.00,"source_ref":"DINING-IT-RC-%d"}
                """.formatted(userId));
        assertThat(rc.get("balance").asDouble()).isEqualTo(1000.00);

        // 3) 建台
        JsonNode table = post(DINING_BASE + "/admin/v1/tables", """
                {"merchant_id":%d,"table_no":"A1","capacity":4}
                """.formatted(merchantId));
        long tableId = table.get("table_id").asLong();

        // 4) 开台（绑会员）→ 点单（1 道菜 ×2，过厨房）
        JsonNode opened = post(DINING_BASE + "/api/v1/dining/table-orders", """
                {"table_id":%d,"guest_count":2,"member_id":%d}
                """.formatted(tableId, userId));
        long tableOrderId = opened.get("table_order_id").asLong();
        JsonNode afterAdd = post(DINING_BASE + "/api/v1/dining/table-orders/" + tableOrderId + "/lines", """
                {"sku_id":%d,"quantity":2,"unit_price":88.00,"requires_kitchen":true}
                """.formatted(dishSku));
        long lineId = afterAdd.get("lines").get(0).get("order_line_id").asLong();

        // 5) KDS 推进到 SERVED
        post(DINING_BASE + "/api/v1/dining/lines/" + lineId + "/send-to-kds", null);
        post(DINING_BASE + "/api/v1/dining/lines/" + lineId + "/advance", "{\"target\":\"COOKING\"}");
        post(DINING_BASE + "/api/v1/dining/lines/" + lineId + "/advance", "{\"target\":\"READY\"}");
        post(DINING_BASE + "/api/v1/dining/lines/" + lineId + "/advance", "{\"target\":\"SERVED\"}");

        // 6) 结账（会员储值抵扣全额 176）
        JsonNode checkout = post(DINING_BASE + "/api/v1/dining/table-orders/" + tableOrderId + "/checkout",
                "{\"use_wallet\":true,\"channel\":\"WECHAT\"}");
        assertThat(checkout.get("status").asText()).isEqualTo("CLOSED");
        assertThat(checkout.get("total_amount").asDouble()).isEqualTo(176.00);
        assertThat(checkout.get("wallet_paid_amount").asDouble()).isEqualTo(176.00);
        assertThat(checkout.get("payable_amount").asDouble()).isEqualTo(0.00);

        // 7) 会员储值余额 1000-176=824
        assertThat(get(MEMBER_BASE + "/api/v1/members/" + userId + "/wallet").get("balance").asDouble())
                .isEqualTo(824.00);

        // 8) 异步 BOM 核减：原料 1000-200×2=600
        awaitUntil(Duration.ofSeconds(60), () -> {
            JsonNode stock = get(SUPPLY_BASE + "/api/v1/supply/stocks?warehouse_id="
                    + warehouseId + "&sku_id=" + ingredientSku);
            return stock.get("quantity").asDouble() == 600.0;
        });
    }
}
