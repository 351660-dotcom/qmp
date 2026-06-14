package com.qmp.hotel.dto;

import lombok.Data;

/**
 * 后台房型上下架请求（PATCH /admin/v1/room-types/{id}/status）。
 */
@Data
public class UpdateStatusRequest {

    /** DRAFT/ON_SALE/OFF_SALE。 */
    private String status;
}
