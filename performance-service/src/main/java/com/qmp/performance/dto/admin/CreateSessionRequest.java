package com.qmp.performance.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台创建场次请求（POST /admin/v1/sessions）。
 */
@Data
public class CreateSessionRequest {

    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("merchant_id")
    private Long merchantId;

    @JsonProperty("sku_id")
    private Long skuId;

    private String name;

    /** SHOW/BOAT/RIDE。 */
    @JsonProperty("session_type")
    private String sessionType;

    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @JsonProperty("base_price")
    private BigDecimal basePrice;

    /** 可选，缺省 DRAFT。 */
    private String status;
}
