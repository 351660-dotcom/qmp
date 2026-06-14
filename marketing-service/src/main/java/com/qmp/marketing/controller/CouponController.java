package com.qmp.marketing.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.marketing.dto.CouponView;
import com.qmp.marketing.dto.IssueCouponRequest;
import com.qmp.marketing.dto.RedeemCouponRequest;
import com.qmp.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券对外接口（13 文档四）。
 */
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/issue")
    public ApiResponse<CouponView> issue(@RequestBody IssueCouponRequest request) {
        return ApiResponse.ok(couponService.issue(request.getTemplateId(), request.getUserId()));
    }

    @GetMapping
    public ApiResponse<List<CouponView>> list(@RequestParam("user_id") Long userId,
                                              @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.ok(couponService.listUserCoupons(userId, status));
    }

    @PostMapping("/{couponId}/redeem")
    public ApiResponse<CouponView> redeem(@PathVariable Long couponId,
                                          @RequestBody RedeemCouponRequest request) {
        return ApiResponse.ok(couponService.redeem(couponId, request.getOrderId()));
    }

    @PostMapping("/{couponId}/revert")
    public ApiResponse<CouponView> revert(@PathVariable Long couponId) {
        return ApiResponse.ok(couponService.revert(couponId));
    }
}
