package com.qmp.supplychain.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台设置库存请求（POST /admin/v1/stocks）。按 (warehouse_id, sku_id) 幂等：存在则置数量，否则建。
 */
@Data
public class UpsertStockRequest {

    @JsonProperty("warehouse_id")
    private Long warehouseId;

    @JsonProperty("sku_id")
    private Long skuId;

    private BigDecimal quantity;

    private String unit;

    @JsonProperty("reorder_point")
    private BigDecimal reorderPoint;
}
