package com.qmp.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * POST /api/v1/inventory/reservations 请求体（见 09 文档五）。
 */
@Getter
@Setter
public class CreateReservationRequest {

    @JsonProperty("reservation_id")
    private String reservationId;

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("sale_date")
    private LocalDate saleDate;

    /** 无分时预约固定为 0。 */
    @JsonProperty("time_slot_id")
    private Long timeSlotId;

    private Integer quantity;
}
