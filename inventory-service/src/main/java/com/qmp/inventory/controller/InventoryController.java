package com.qmp.inventory.controller;

import com.qmp.inventory.dto.AvailabilityResponse;
import com.qmp.inventory.dto.CreateReservationRequest;
import com.qmp.inventory.dto.ReservationResponse;
import com.qmp.inventory.service.InventoryService;
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
 * 库存中心对外接口（见 09 文档五）。
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/availability")
    public ApiResponse<AvailabilityResponse> getAvailability(
            @RequestParam("sku_id") Long skuId,
            @RequestParam("sale_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate,
            @RequestParam(value = "time_slot_id", required = false) Long timeSlotId) {
        return ApiResponse.ok(inventoryService.getAvailability(skuId, saleDate, timeSlotId));
    }

    @PostMapping("/reservations")
    public ApiResponse<ReservationResponse> createReservation(@RequestBody CreateReservationRequest request) {
        return ApiResponse.ok(inventoryService.createReservation(request));
    }

    @PostMapping("/reservations/{reservationId}/confirm")
    public ApiResponse<ReservationResponse> confirm(@PathVariable String reservationId) {
        return ApiResponse.ok(inventoryService.confirmReservation(reservationId));
    }

    @PostMapping("/reservations/{reservationId}/release")
    public ApiResponse<ReservationResponse> release(
            @PathVariable String reservationId,
            @RequestParam(value = "quantity", required = false) Integer quantity) {
        return ApiResponse.ok(inventoryService.releaseReservation(reservationId, quantity));
    }
}
