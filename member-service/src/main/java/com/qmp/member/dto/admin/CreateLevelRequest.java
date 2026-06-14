package com.qmp.member.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 后台创建会员等级请求（POST /admin/v1/levels）。
 */
@Data
public class CreateLevelRequest {

    @JsonProperty("level_name")
    private String levelName;

    @JsonProperty("min_growth_value")
    private Integer minGrowthValue;

    /** 权益 JSON：point_rate / 折扣率 / 专属券模板等。 */
    private JsonNode benefits;
}
