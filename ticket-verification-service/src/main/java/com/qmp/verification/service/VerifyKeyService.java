package com.qmp.verification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.verification.entity.VerifyKey;
import com.qmp.verification.mapper.VerifyKeyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 核销码签名密钥管理（按租户/景区，带版本轮换）。
 * 签发取景区 ACTIVE 密钥；验签按 kid 取密钥；轮换生成新 ACTIVE、旧密钥置 RETIRED 仍可验旧码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private final VerifyKeyMapper verifyKeyMapper;

    /** 取某景区当前 ACTIVE 密钥；未配置返回 null（调用方回落全局密钥）。 */
    public VerifyKey resolveActive(Long scenicId) {
        if (scenicId == null) {
            return null;
        }
        return verifyKeyMapper.selectOne(new LambdaQueryWrapper<VerifyKey>()
                .eq(VerifyKey::getScenicId, scenicId)
                .eq(VerifyKey::getStatus, "ACTIVE")
                .last("LIMIT 1"));
    }

    /** 按 kid 取密钥（验签用，含已 RETIRED 的，使旧码仍可验）。 */
    public VerifyKey findById(Long kid) {
        return (kid == null || kid == 0L) ? null : verifyKeyMapper.selectById(kid);
    }

    /**
     * 轮换某景区密钥：当前 ACTIVE 置 RETIRED（旧码仍可按 kid 验签），生成新版本 ACTIVE。返回新密钥。
     */
    @Transactional
    public VerifyKey rotate(Long scenicId) {
        VerifyKey current = resolveActive(scenicId);
        int nextVersion = (current != null ? current.getKeyVersion() : 0) + 1;
        if (current != null) {
            current.setStatus("RETIRED");
            verifyKeyMapper.updateById(current);
        }
        VerifyKey fresh = new VerifyKey();
        fresh.setTenantId(TenantContext.get());
        fresh.setScenicId(scenicId);
        fresh.setKeyVersion(nextVersion);
        fresh.setSecret(randomSecret());
        fresh.setStatus("ACTIVE");
        verifyKeyMapper.insert(fresh);
        log.info("verify_key 轮换: scenicId={}, newVersion={}, kid={}", scenicId, nextVersion, fresh.getId());
        return fresh;
    }

    private String randomSecret() {
        byte[] buf = new byte[32];
        RANDOM.nextBytes(buf);
        return B64.encodeToString(buf);
    }
}
