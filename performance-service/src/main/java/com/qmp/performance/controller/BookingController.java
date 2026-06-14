package com.qmp.performance.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.performance.dto.BookSeatRequest;
import com.qmp.performance.dto.BookSessionRequest;
import com.qmp.performance.dto.BookingResponse;
import com.qmp.performance.dto.PayRequest;
import com.qmp.performance.dto.PayResponse;
import com.qmp.performance.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演出/游船/游乐预订对外接口（14 文档一/二）。
 */
@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings/session")
    public ApiResponse<BookingResponse> bookSession(@RequestBody BookSessionRequest request) {
        return ApiResponse.ok(bookingService.bookSession(request));
    }

    @PostMapping("/bookings/seat")
    public ApiResponse<BookingResponse> bookSeat(@RequestBody BookSeatRequest request) {
        return ApiResponse.ok(bookingService.bookSeat(request));
    }

    @PostMapping("/bookings/{bookingId}/pay")
    public ApiResponse<PayResponse> pay(@PathVariable Long bookingId, @RequestBody PayRequest request) {
        return ApiResponse.ok(bookingService.pay(bookingId, request.getChannel()));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long bookingId) {
        bookingService.cancel(bookingId);
        return ApiResponse.ok();
    }

    @GetMapping("/bookings/{bookingId}")
    public ApiResponse<BookingResponse> get(@PathVariable Long bookingId) {
        return ApiResponse.ok(bookingService.getBooking(bookingId));
    }
}
