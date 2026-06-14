package com.qmp.order.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.order.dto.admin.OrderSummaryView;
import com.qmp.order.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单中心后台管理接口（{@code /admin/v1}）。运营后台据此查看订单。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping("/orders")
    public ApiResponse<List<OrderSummaryView>> listOrders(
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.ok(adminOrderService.listOrders(status));
    }
}
