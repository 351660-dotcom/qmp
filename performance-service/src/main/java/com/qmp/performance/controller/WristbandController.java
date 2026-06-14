package com.qmp.performance.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.performance.dto.IssueWristbandRequest;
import com.qmp.performance.dto.WristbandTxnRequest;
import com.qmp.performance.dto.WristbandView;
import com.qmp.performance.service.WristbandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 手牌/腕带二次消费接口（14 文档四）。
 */
@RestController
@RequestMapping("/api/v1/performance/wristbands")
@RequiredArgsConstructor
public class WristbandController {

    private final WristbandService wristbandService;

    @PostMapping
    public ApiResponse<WristbandView> issue(@RequestBody IssueWristbandRequest request) {
        return ApiResponse.ok(wristbandService.issue(request));
    }

    @GetMapping("/{wristbandId}")
    public ApiResponse<WristbandView> get(@PathVariable Long wristbandId) {
        return ApiResponse.ok(wristbandService.getView(wristbandId));
    }

    @PostMapping("/{wristbandId}/recharge")
    public ApiResponse<Map<String, Object>> recharge(@PathVariable Long wristbandId,
                                                     @RequestBody WristbandTxnRequest request) {
        return ApiResponse.ok(Map.of("balance",
                wristbandService.recharge(wristbandId, request.getAmount(), request.getSourceRef())));
    }

    @PostMapping("/{wristbandId}/consume")
    public ApiResponse<Map<String, Object>> consume(@PathVariable Long wristbandId,
                                                    @RequestBody WristbandTxnRequest request) {
        return ApiResponse.ok(Map.of("balance",
                wristbandService.consume(wristbandId, request.getAmount(), request.getMerchantId(),
                        request.getSourceRef())));
    }
}
