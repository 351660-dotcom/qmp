package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 核销规则之「核销介质」端到端：后台建带核销介质 [QR_CODE,IC_CARD,FACE] 的产品+票种 →
 * SKU 查询返回该核销介质列表（供核验终端/前端识别可用介质）。
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("核销规则-核销介质：SKU 查询暴露产品的核销介质列表")
class VerificationMediumIT extends E2eSupport {

    @Test
    void skuExposesProductVerificationMedium() {
        awaitServiceUp(PRODUCT_BASE + "/actuator/health");

        JsonNode product = post(PRODUCT_BASE + "/admin/v1/products", """
                {"name":"核销介质测试产品","scenic_id":%d,"merchant_id":%d,
                 "valid_period_rule":{"type":"FIXED_DATE","valid_days":1},"real_name_rule":"NONE",
                 "verification_medium":["QR_CODE","IC_CARD","FACE"],"status":"ON_SALE"}
                """.formatted(SCENIC_ID, MERCHANT_ID));
        long productId = product.get("product_id").asLong();
        long skuId = post(PRODUCT_BASE + "/admin/v1/skus",
                "{\"product_id\":%d,\"ticket_type\":\"ADULT\",\"requires_time_slot\":false}".formatted(productId))
                .get("sku_id").asLong();

        JsonNode sku = get(PRODUCT_BASE + "/api/v1/skus/" + skuId);
        JsonNode media = sku.get("verification_medium");
        assertThat(media).isNotNull();
        assertThat(media.isArray()).isTrue();
        assertThat(media.toString()).contains("QR_CODE").contains("IC_CARD").contains("FACE");
    }
}
