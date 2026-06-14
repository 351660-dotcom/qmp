package com.qmp.supplychain.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 后台创建仓库请求（POST /admin/v1/warehouses）。
 */
@Data
public class CreateWarehouseRequest {

    /** HQ/CENTRAL_KITCHEN/STORE。 */
    @JsonProperty("owner_scope")
    private String ownerScope;

    /** owner_scope=STORE 时必填。 */
    @JsonProperty("merchant_id")
    private Long merchantId;

    private String name;
}
