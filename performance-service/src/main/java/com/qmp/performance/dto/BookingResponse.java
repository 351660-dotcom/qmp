package com.qmp.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 预订单响应。
 */
@Getter
@Builder
public class BookingResponse {

    @JsonProperty("booking_id")
    private Long bookingId;

    private String status;

    @JsonProperty("session_id")
    private Long sessionId;

    @JsonProperty("bucket_ref")
    private String bucketRef;

    @JsonProperty("seat_id")
    private String seatId;

    private Integer quantity;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("payment_id")
    private String paymentId;
}
