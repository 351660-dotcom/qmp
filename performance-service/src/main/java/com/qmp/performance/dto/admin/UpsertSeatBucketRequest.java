package com.qmp.performance.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 后台创建/调整座位库存桶请求（POST /admin/v1/seat-buckets）。按 (session_id, seat_id) 幂等。
 * capacity 通常为 1（剧院座位）或舱位定员。
 */
@Data
public class UpsertSeatBucketRequest {

    @JsonProperty("session_id")
    private Long sessionId;

    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("seat_id")
    private String seatId;

    private Integer capacity;
}
