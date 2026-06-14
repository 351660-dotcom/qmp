package com.qmp.marketing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 秒杀抢购请求。
 */
@Data
public class SnapRequest {

    @JsonProperty("user_id")
    private Long userId;
}
