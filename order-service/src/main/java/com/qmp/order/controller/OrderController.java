package com.qmp.order.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.order.dto.CreateOrderRequest;
import com.qmp.order.dto.CreateOrderResponse;
import com.qmp.order.dto.OrderDetailResponse;
import com.qmp.order.dto.PayRequest;
import com.qmp.order.dto.PayResponse;
import com.qmp.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单编排中心对外接口（09 文档八，C 端入口）。
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<CreateOrderResponse> create(@RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.createOrder(request));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<PayResponse> pay(@PathVariable Long orderId, @RequestBody PayRequest request) {
        return ApiResponse.ok(orderService.pay(orderId, request.getChannel()));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> detail(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getOrder(orderId));
    }
}
