package com.qmp.marketing.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.marketing.dto.SnapRequest;
import com.qmp.marketing.dto.SnapResponse;
import com.qmp.marketing.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀对外接口（13 文档 5.1）。
 */
@RestController
@RequestMapping("/api/v1/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @PostMapping("/{activityId}/snap")
    public ApiResponse<SnapResponse> snap(@PathVariable Long activityId, @RequestBody SnapRequest request) {
        return ApiResponse.ok(seckillService.snap(activityId, request.getUserId()));
    }

    @PostMapping("/reservations/{reservationId}/release")
    public ApiResponse<Void> release(@PathVariable String reservationId) {
        seckillService.release(reservationId);
        return ApiResponse.ok();
    }
}
