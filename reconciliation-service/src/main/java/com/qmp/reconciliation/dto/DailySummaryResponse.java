package com.qmp.reconciliation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 按日（可选商户）对账汇总。
 */
@Getter
@Builder
public class DailySummaryResponse {

    @JsonProperty("recon_date")
    private LocalDate reconDate;

    @JsonProperty("merchant_id")
    private Long merchantId;

    /** 收款合计（direction=IN）。 */
    @JsonProperty("in_total")
    private BigDecimal inTotal;

    /** 出账合计（direction=OUT，退款等）。 */
    @JsonProperty("out_total")
    private BigDecimal outTotal;

    /** 净额 = in_total - out_total。 */
    private BigDecimal net;

    private Integer count;

    /** 各资金来源金额明细（PAYMENT/REFUND/WALLET/WRISTBAND）。 */
    @JsonProperty("by_source")
    private Map<String, BigDecimal> bySource;
}
