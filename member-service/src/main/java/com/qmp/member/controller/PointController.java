package com.qmp.member.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.member.dto.PointBalanceResponse;
import com.qmp.member.dto.RedeemPointRequest;
import com.qmp.member.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员积分接口（13 文档 1.3）。
 */
@RestController
@RequestMapping("/api/v1/members/{userId}/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping
    public ApiResponse<PointBalanceResponse> balance(@PathVariable Long userId) {
        return ApiResponse.ok(PointBalanceResponse.builder()
                .userId(userId).balance(pointService.getBalance(userId)).build());
    }

    @PostMapping("/redeem")
    public ApiResponse<PointBalanceResponse> redeem(@PathVariable Long userId,
                                                    @RequestBody RedeemPointRequest request) {
        int balance = pointService.redeem(userId, request.getPoints(), request.getMerchantId(), request.getSourceRef());
        return ApiResponse.ok(PointBalanceResponse.builder().userId(userId).balance(balance).build());
    }
}
