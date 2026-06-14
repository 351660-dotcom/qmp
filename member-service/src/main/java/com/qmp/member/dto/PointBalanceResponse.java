package com.qmp.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 积分余额响应。
 */
@Getter
@Builder
public class PointBalanceResponse {

    @JsonProperty("user_id")
    private Long userId;

    private Integer balance;
}
