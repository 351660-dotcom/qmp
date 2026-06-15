package com.qmp.payment.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台设置商户分账抽成比例请求（PUT /admin/v1/merchant-commissions）。
 */
@Data
public class UpsertCommissionRequest {

    @JsonProperty("merchant_id")
    private Long merchantId;

    /** 平台抽成比例 0..1（如 0.06 = 6%）。 */
    @JsonProperty("commission_rate")
    private BigDecimal commissionRate;
}
