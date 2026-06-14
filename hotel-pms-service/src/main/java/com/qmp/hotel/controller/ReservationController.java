package com.qmp.hotel.controller;

import com.qmp.hotel.dto.AvailabilityResponse;
import com.qmp.hotel.dto.CreateReservationRequest;
import com.qmp.hotel.dto.PayRequest;
import com.qmp.hotel.dto.PayResponse;
import com.qmp.hotel.dto.ReservationResponse;
import com.qmp.hotel.service.HotelInventoryService;
import com.qmp.hotel.service.HotelReservationService;
import com.qmp.kernel.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 酒店预订对外接口（11 文档 2.x）。
 */
@RestController
@RequestMapping("/api/v1/hotel")
@RequiredArgsConstructor
public class ReservationController {

    private final HotelReservationService reservationService;
    private final HotelInventoryService inventoryService;

    @GetMapping("/availability")
    public ApiResponse<AvailabilityResponse> availability(
            @RequestParam("sku_id") Long skuId,
            @RequestParam("check_in_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam("check_out_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ApiResponse.ok(inventoryService.availability(skuId, checkIn, checkOut));
    }

    @PostMapping("/reservations")
    public ApiResponse<ReservationResponse> create(@RequestBody CreateReservationRequest request) {
        return ApiResponse.ok(reservationService.createReservation(request));
    }

    @PostMapping("/reservations/{reservationId}/pay")
    public ApiResponse<PayResponse> pay(@PathVariable Long reservationId, @RequestBody PayRequest request) {
        return ApiResponse.ok(reservationService.pay(reservationId, request.getChannel()));
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long reservationId) {
        reservationService.cancel(reservationId);
        return ApiResponse.ok();
    }

    @GetMapping("/reservations/{reservationId}")
    public ApiResponse<ReservationResponse> get(@PathVariable Long reservationId) {
        return ApiResponse.ok(reservationService.getReservation(reservationId));
    }
}
