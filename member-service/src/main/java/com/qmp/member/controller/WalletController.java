package com.qmp.member.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.member.dto.DeductWalletRequest;
import com.qmp.member.dto.RechargeRequest;
import com.qmp.member.dto.WalletBalanceResponse;
import com.qmp.member.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员储值接口（13 文档 1.4 / 12 文档 DeductWallet）。
 */
@RestController
@RequestMapping("/api/v1/members/{userId}/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ApiResponse<WalletBalanceResponse> balance(@PathVariable Long userId) {
        return ApiResponse.ok(WalletBalanceResponse.builder()
                .userId(userId).balance(walletService.getBalance(userId)).build());
    }

    @PostMapping("/recharge")
    public ApiResponse<WalletBalanceResponse> recharge(@PathVariable Long userId,
                                                       @RequestBody RechargeRequest request) {
        var balance = walletService.recharge(userId, request.getAmount(), request.getSourceRef());
        return ApiResponse.ok(WalletBalanceResponse.builder().userId(userId).balance(balance).build());
    }

    @PostMapping("/deduct")
    public ApiResponse<WalletBalanceResponse> deduct(@PathVariable Long userId,
                                                     @RequestBody DeductWalletRequest request) {
        var balance = walletService.deduct(userId, request.getAmount(), request.getMerchantId(), request.getSourceRef());
        return ApiResponse.ok(WalletBalanceResponse.builder().userId(userId).balance(balance).build());
    }
}
