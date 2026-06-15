package com.qmp.verification.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.verification.entity.VerifyKey;
import com.qmp.verification.service.VerifyKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台核销码密钥管理（{@code /admin/v1}，由 inventory-kernel AdminAuthFilter 保护）。
 * 按景区轮换签名密钥：生成新 ACTIVE 版本，旧版本置 RETIRED（旧码仍可验签）。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminVerifyKeyController {

    private final VerifyKeyService verifyKeyService;

    @PostMapping("/verify-keys/rotate")
    public ApiResponse<Map<String, Object>> rotate(@RequestParam("scenic_id") Long scenicId) {
        VerifyKey key = verifyKeyService.rotate(scenicId);
        return ApiResponse.ok(Map.of("kid", key.getId(), "key_version", key.getKeyVersion()));
    }
}
