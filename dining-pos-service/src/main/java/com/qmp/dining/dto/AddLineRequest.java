package com.qmp.dining.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 加菜/点单请求（12 文档 1.4）。v1 单价由 POS 端（菜单）传入快照，未接价格中心。
 */
@Data
public class AddLineRequest {

    @JsonProperty("sku_id")
    private Long skuId;

    private Integer quantity;

    @JsonProperty("unit_price")
    private BigDecimal unitPrice;

    private String remark;

    /** CUSTOMER_QR/STAFF_PDA，缺省 STAFF_PDA。 */
    private String source;

    /** 是否需过厨房，缺省 true。 */
    @JsonProperty("requires_kitchen")
    private Boolean requiresKitchen;
}
