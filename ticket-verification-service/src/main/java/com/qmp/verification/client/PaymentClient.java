package com.qmp.verification.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 同步调用 payment-service 发起退款（09 文档六 {@code POST /api/v1/payments/{payment_id}/refunds}）。
 * 幂等键 = credential_id，重复调用返回同一 refund_id。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

    private final RestTemplate restTemplate;

    @Value("${verification.client.payment-base-url:http://localhost:8085}")
    private String paymentBaseUrl;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RefundResult {
        @JsonProperty("refund_id")
        private Long refundId;
        private String status;
    }

    public Long createRefund(Long tenantId, String paymentId, Long credentialId, BigDecimal amount) {
        String url = paymentBaseUrl + "/api/v1/payments/" + paymentId + "/refunds";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", String.valueOf(tenantId));

        Map<String, Object> body = Map.of("credential_id", credentialId, "amount", amount);

        ResponseEntity<ApiResult<RefundResult>> resp = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<>() {
                });

        ApiResult<RefundResult> result = resp.getBody();
        if (result == null || result.getData() == null) {
            throw new IllegalStateException("payment-service 退款响应为空: paymentId=" + paymentId);
        }
        log.info("payment-service 退款成功: paymentId={}, credentialId={}, refundId={}",
                paymentId, credentialId, result.getData().getRefundId());
        return result.getData().getRefundId();
    }
}
