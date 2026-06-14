package com.qmp.performance.dto;

import lombok.Data;

/**
 * 场次上下架/取消请求（PATCH /admin/v1/sessions/{id}/status）。
 */
@Data
public class UpdateStatusRequest {

    /** DRAFT/ON_SALE/CANCELLED/CLOSED。 */
    private String status;
}
