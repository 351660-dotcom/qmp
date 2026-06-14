package com.qmp.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 库存预占相关接口的统一响应（见 09 文档五）。
 */
@Getter
@Builder
public class ReservationResponse {

    @JsonProperty("reservation_id")
    private String reservationId;

    /** HOLDING/CONFIRMED/RELEASED/EXPIRED */
    private String status;

    @JsonProperty("hold_expire_at")
    private LocalDateTime holdExpireAt;
}
