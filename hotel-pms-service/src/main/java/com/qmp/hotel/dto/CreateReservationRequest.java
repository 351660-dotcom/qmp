package com.qmp.hotel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建预订单请求（11 文档 2.1）。连住区间 [check_in_date, check_out_date)。
 */
@Data
public class CreateReservationRequest {

    @JsonProperty("user_id")
    private Long userId;

    /** 房型 SKU。 */
    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("check_in_date")
    private LocalDate checkInDate;

    @JsonProperty("check_out_date")
    private LocalDate checkOutDate;

    /** 预订间数，缺省 1。 */
    @JsonProperty("room_count")
    private Integer roomCount;
}
