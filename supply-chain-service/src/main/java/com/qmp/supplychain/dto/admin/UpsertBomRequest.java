package com.qmp.supplychain.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台设置菜品 BOM 请求（POST /admin/v1/boms）。按 output_sku_id 幂等。
 */
@Data
public class UpsertBomRequest {

    @JsonProperty("output_sku_id")
    private Long outputSkuId;

    @JsonProperty("output_quantity")
    private BigDecimal outputQuantity;

    /** 原料用量 JSON 数组：[{material_sku_id, quantity, unit}]。 */
    private JsonNode materials;
}
