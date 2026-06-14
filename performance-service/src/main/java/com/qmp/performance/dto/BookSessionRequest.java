package com.qmp.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 场次预订请求（无座位业态：游船/游乐）。
 */
@Data
public class BookSessionRequest {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("session_id")
    private Long sessionId;

    private Integer quantity;
}
