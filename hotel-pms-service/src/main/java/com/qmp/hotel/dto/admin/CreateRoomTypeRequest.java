package com.qmp.hotel.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台创建房型请求（POST /admin/v1/room-types）。
 */
@Data
public class CreateRoomTypeRequest {

    private String name;

    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    /** 库存桶关联键（与门票 SKU 同序列）。 */
    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("base_price")
    private BigDecimal basePrice;

    /** 可选，缺省 DRAFT。 */
    private String status;
}
