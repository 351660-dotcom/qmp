package com.qmp.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * GET /api/v1/members/{user_id}/status 响应（见 09 文档四）。
 */
@Getter
@Builder
public class MemberStatusResponse {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("is_member")
    private boolean isMember;
}
