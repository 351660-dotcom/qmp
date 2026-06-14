package com.qmp.dining.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 后台创建桌台请求（POST /admin/v1/tables）。
 */
@Data
public class CreateTableRequest {

    @JsonProperty("merchant_id")
    private Long merchantId;

    @JsonProperty("area_id")
    private Long areaId;

    @JsonProperty("table_no")
    private String tableNo;

    private Integer capacity;
}
