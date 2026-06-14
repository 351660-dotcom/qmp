package com.qmp.reconciliation.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.reconciliation.dto.DailySummaryResponse;
import com.qmp.reconciliation.entity.ReconTransaction;
import com.qmp.reconciliation.service.ReconService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 跨业态统一对账查询接口（④）。
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconService reconService;

    @GetMapping("/daily")
    public ApiResponse<DailySummaryResponse> daily(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "merchant_id", required = false) Long merchantId) {
        return ApiResponse.ok(reconService.dailySummary(date, merchantId));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<ReconTransaction>> transactions(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "merchant_id", required = false) Long merchantId) {
        return ApiResponse.ok(reconService.listTransactions(date, merchantId));
    }
}
