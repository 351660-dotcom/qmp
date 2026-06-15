package com.qmp.verification.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.verification.entity.VerifyKey;
import com.qmp.verification.error.VerificationErrorCode;
import com.qmp.verification.service.VerifyKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 核销凭证码（verify_code）签名器（07 文档 2.3「签名串，支持离线验签」）。
 *
 * <p>格式：{@code base64url(payloadJson) + "." + hex(HMAC-SHA256(payloadJson, secret))}。
 * payloadJson 形如 {@code {"cid":9001001,"oii":"OI-...","sku":1001,"d":"2026-07-01","kid":123}}。
 * 验签时重算 HMAC 并常量时间比对，签名不符抛 {@link VerificationErrorCode#INVALID_SIGNATURE}。</p>
 *
 * <p><b>按租户/景区密钥 + 轮换</b>：{@code kid} 指向 {@code verify_key.id}。签发时取该景区 ACTIVE
 * 密钥（{@link VerifyKeyService#resolveActive}）签名并把 kid 写入码；验签时按码内 kid 取对应密钥
 * （含已轮换 RETIRED 的，使旧码仍可验）。{@code kid=0} 表示该景区未配置密钥，回落全局密钥。
 * 离线核验：边缘节点持对应 kid 的密钥即可本地验签。</p>
 */
@Component
@RequiredArgsConstructor
public class VerifyCodeSigner {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final VerifyKeyService verifyKeyService;

    @Value("${verification.verify-code.secret:scenic-saas-v1-verify-secret}")
    private String globalSecret;

    /** 凭证码内嵌的最小载荷（核验时据此定位凭证）。{@code kid}=签名密钥 id（0=全局密钥）。 */
    public record Payload(long cid, String oii, long sku, String d, long kid) {
    }

    /** 用指定密钥签名（{@code secret} 为 null 时用全局密钥）。 */
    public String sign(Payload payload, String secret) {
        String payloadJson = toJson(payload);
        String body = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return body + "." + hmacHex(body, secret != null ? secret : globalSecret);
    }

    /**
     * 验签并解析载荷。签名缺失/格式错误/不匹配/kid 对应密钥不存在均抛
     * {@link VerificationErrorCode#INVALID_SIGNATURE}。密钥按码内 kid 解析（kid=0 用全局密钥）。
     */
    public Payload verify(String verifyCode) {
        if (verifyCode == null) {
            throw new BizException(VerificationErrorCode.INVALID_SIGNATURE);
        }
        int dot = verifyCode.lastIndexOf('.');
        if (dot <= 0 || dot == verifyCode.length() - 1) {
            throw new BizException(VerificationErrorCode.INVALID_SIGNATURE);
        }
        String body = verifyCode.substring(0, dot);
        String sig = verifyCode.substring(dot + 1);

        // 先解析载荷（未信任）以取 kid，再据 kid 解析密钥；kid 被篡改会导致密钥不符、HMAC 比对失败
        Payload payload = decodeBody(body);
        String secret = resolveSecret(payload.kid());

        String expected = hmacHex(body, secret);
        if (!constantTimeEquals(expected, sig)) {
            throw new BizException(VerificationErrorCode.INVALID_SIGNATURE);
        }
        return payload;
    }

    private String resolveSecret(long kid) {
        if (kid == 0L) {
            return globalSecret;
        }
        VerifyKey key = verifyKeyService.findById(kid);
        if (key == null) {
            throw new BizException(VerificationErrorCode.INVALID_SIGNATURE);
        }
        return key.getSecret();
    }

    private Payload decodeBody(String body) {
        try {
            String payloadJson = new String(URL_DECODER.decode(body), StandardCharsets.UTF_8);
            return objectMapper.readValue(payloadJson, Payload.class);
        } catch (Exception e) {
            throw new BizException(VerificationErrorCode.INVALID_SIGNATURE);
        }
    }

    private String toJson(Payload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("生成 verify_code 失败", e);
        }
    }

    private String hmacHex(String body, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
