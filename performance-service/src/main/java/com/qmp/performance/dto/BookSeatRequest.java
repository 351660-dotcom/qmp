package com.qmp.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 选座预订请求（剧院选座/游船舱位）。
 */
@Data
public class BookSeatRequest {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("session_id")
    private Long sessionId;

    @JsonProperty("seat_id")
    private String seatId;
}
