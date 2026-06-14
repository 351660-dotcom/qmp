package com.qmp.hotel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预订单响应。
 */
@Getter
@Builder
public class ReservationResponse {

    @JsonProperty("reservation_id")
    private Long reservationId;

    private String status;

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("check_in_date")
    private LocalDate checkInDate;

    @JsonProperty("check_out_date")
    private LocalDate checkOutDate;

    private Integer nights;

    @JsonProperty("room_count")
    private Integer roomCount;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("payment_id")
    private String paymentId;
}
