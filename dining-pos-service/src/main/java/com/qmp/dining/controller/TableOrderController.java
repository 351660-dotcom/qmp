package com.qmp.dining.controller;

import com.qmp.dining.dto.AddLineRequest;
import com.qmp.dining.dto.AdvanceLineRequest;
import com.qmp.dining.dto.CheckoutRequest;
import com.qmp.dining.dto.CheckoutResponse;
import com.qmp.dining.dto.OpenTableRequest;
import com.qmp.dining.dto.TableOrderView;
import com.qmp.dining.service.TableOrderService;
import com.qmp.kernel.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 餐饮台账对外接口（12 文档一/二/四）。
 */
@RestController
@RequestMapping("/api/v1/dining")
@RequiredArgsConstructor
public class TableOrderController {

    private final TableOrderService tableOrderService;

    @PostMapping("/table-orders")
    public ApiResponse<TableOrderView> open(@RequestBody OpenTableRequest request) {
        return ApiResponse.ok(tableOrderService.openTable(request));
    }

    @GetMapping("/table-orders/{tableOrderId}")
    public ApiResponse<TableOrderView> get(@PathVariable Long tableOrderId) {
        return ApiResponse.ok(tableOrderService.getView(tableOrderId));
    }

    @PostMapping("/table-orders/{tableOrderId}/lines")
    public ApiResponse<TableOrderView> addLine(@PathVariable Long tableOrderId,
                                               @RequestBody AddLineRequest request) {
        return ApiResponse.ok(tableOrderService.addLine(tableOrderId, request));
    }

    @PostMapping("/lines/{orderLineId}/send-to-kds")
    public ApiResponse<Void> sendToKds(@PathVariable Long orderLineId) {
        tableOrderService.sendToKds(orderLineId);
        return ApiResponse.ok();
    }

    @PostMapping("/lines/{orderLineId}/advance")
    public ApiResponse<Void> advance(@PathVariable Long orderLineId, @RequestBody AdvanceLineRequest request) {
        tableOrderService.advanceLine(orderLineId, request.getTarget());
        return ApiResponse.ok();
    }

    @PostMapping("/lines/{orderLineId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long orderLineId) {
        tableOrderService.cancelLine(orderLineId);
        return ApiResponse.ok();
    }

    @PostMapping("/lines/{orderLineId}/return")
    public ApiResponse<Void> returnLine(@PathVariable Long orderLineId) {
        tableOrderService.returnLine(orderLineId);
        return ApiResponse.ok();
    }

    @PostMapping("/table-orders/{tableOrderId}/checkout")
    public ApiResponse<CheckoutResponse> checkout(@PathVariable Long tableOrderId,
                                                  @RequestBody CheckoutRequest request) {
        return ApiResponse.ok(tableOrderService.checkout(tableOrderId, request));
    }
}
