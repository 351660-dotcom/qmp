package com.qmp.performance.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.performance.dto.admin.CreateSessionRequest;
import com.qmp.performance.dto.admin.UpsertSeatBucketRequest;
import com.qmp.performance.dto.admin.UpsertSessionBucketRequest;
import com.qmp.performance.dto.UpdateStatusRequest;
import com.qmp.performance.service.AdminPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 演出后台管理接口（{@code /admin/v1}）。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminPerformanceController {

    private final AdminPerformanceService adminPerformanceService;

    @PostMapping("/sessions")
    public ApiResponse<Map<String, Object>> createSession(@RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(Map.of("session_id", adminPerformanceService.createSession(request)));
    }

    @PatchMapping("/sessions/{sessionId}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long sessionId,
                                          @RequestBody UpdateStatusRequest request) {
        adminPerformanceService.updateStatus(sessionId, request.getStatus());
        return ApiResponse.ok();
    }

    @PostMapping("/session-buckets")
    public ApiResponse<Map<String, Object>> upsertSessionBucket(@RequestBody UpsertSessionBucketRequest request) {
        return ApiResponse.ok(Map.of("bucket_id", adminPerformanceService.upsertSessionBucket(request)));
    }

    @PostMapping("/seat-buckets")
    public ApiResponse<Map<String, Object>> upsertSeatBucket(@RequestBody UpsertSeatBucketRequest request) {
        return ApiResponse.ok(Map.of("bucket_id", adminPerformanceService.upsertSeatBucket(request)));
    }
}
