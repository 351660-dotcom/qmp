package com.qmp.performance.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 后台创建/调整场次库存桶请求（POST /admin/v1/session-buckets）。按 session_id 幂等。
 */
@Data
public class UpsertSessionBucketRequest {

    @JsonProperty("session_id")
    private Long sessionId;

    @JsonProperty("scenic_id")
    private Long scenicId;

    @JsonProperty("total_quota")
    private Integer totalQuota;
}
