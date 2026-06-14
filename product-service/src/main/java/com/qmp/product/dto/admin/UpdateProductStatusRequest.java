package com.qmp.product.dto.admin;

import lombok.Data;

/**
 * 后台上下架请求（PATCH /admin/v1/products/{id}/status）。
 */
@Data
public class UpdateProductStatusRequest {

    /** DRAFT/PENDING_REVIEW/ON_SALE/OFF_SALE。 */
    private String status;
}
