package com.qmp.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 办理手牌请求（14 文档 4.1）。
 */
@Data
public class IssueWristbandRequest {

    @JsonProperty("scenic_id")
    private Long scenicId;

    /** 可选：出示会员码绑定。 */
    @JsonProperty("user_id")
    private Long userId;

    /** 可选：办理时初始充值额。 */
    @JsonProperty("initial_amount")
    private BigDecimal initialAmount;
}
