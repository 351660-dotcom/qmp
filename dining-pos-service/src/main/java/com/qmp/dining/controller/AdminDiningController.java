package com.qmp.dining.controller;

import com.qmp.dining.dto.admin.CreateTableRequest;
import com.qmp.dining.entity.DiningTable;
import com.qmp.dining.service.TableService;
import com.qmp.kernel.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 餐饮后台管理接口（{@code /admin/v1}）：桌台维护。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminDiningController {

    private final TableService tableService;

    @PostMapping("/tables")
    public ApiResponse<Map<String, Object>> createTable(@RequestBody CreateTableRequest request) {
        return ApiResponse.ok(Map.of("table_id", tableService.createTable(request)));
    }

    @GetMapping("/tables")
    public ApiResponse<List<DiningTable>> listTables(
            @RequestParam(value = "merchant_id", required = false) Long merchantId) {
        return ApiResponse.ok(tableService.listTables(merchantId));
    }
}
