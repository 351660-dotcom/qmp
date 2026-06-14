package com.qmp.dining.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 开台请求（12 文档 1.3）。零售即时收银可不传 table_id。
 */
@Data
public class OpenTableRequest {

    @JsonProperty("table_id")
    private Long tableId;

    @JsonProperty("guest_count")
    private Integer guestCount;

    /** 关联会员（可选，用于储值抵扣与积分）。 */
    @JsonProperty("member_id")
    private Long memberId;
}
