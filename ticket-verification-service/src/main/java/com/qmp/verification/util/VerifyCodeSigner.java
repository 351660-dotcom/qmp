package com.qmp.verification.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.verification.error.VerificationErrorCode;
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
 * payloadJson 形如 {@code {"cid":9001001,"oii":"OI-...","sku":1001,"d":"2026-07-01"}}。
 * 验签时重算 HMAC 并常量时间比对，签名不符抛 {@link VerificationErrorCode#INVALID_SIGNATURE}。</p>
 *
 * <p>离线核验诉求（07 文档 3 第 2 点）：边缘节点持有同一 secret 即可本地验签，无需回查中心。
 * v1 secret 来自配置；生产应按租户/景区下发独立密钥并轮换（见 CLAUDE.md）。</p>
 */
@Component
@RequiredArgsConstructor
public class VerifyCodeSigner {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;

    @Value("${verification.verify-code.secret:scenic-saas-v1-verify-secret}")
    private String secret;

    /** 凭证码内嵌的最小载荷（核验时据此定位凭证）。 */
    public record Payload(long cid, String oii, long sku, String d) {
    }

    public String sign(Payload payload) {
        String payloadJson = toJson(payload);
        String body = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return body + "." + hmacHex(body);
    }

    /**
     * 验签并解析载荷。签名缺失/格式错误/不匹配均抛 {@link VerificationErrorCode#INVALID_SIGNATURE}。
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

        String expected = hmacHex(body);
        if (!constantTimeEquals(expected, sig)) {
            throw new BizException(VerificationErrorCode.INVALID_SIGNATURE);
        }
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

    private String hmacHex(String body) {
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
