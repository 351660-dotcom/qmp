package com.qmp.performance.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qmp.kernel.context.TenantContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调用 payment-service 创建支付单（幂等键 order_id = booking_id）。payment 不感知业态差异。
 */
@Component
@RequiredArgsConstructor
public class PaymentClient {

    private final RestTemplate restTemplate;

    @Value("${performance.client.payment-base-url:http://localhost:8085}")
    private String baseUrl;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentView {
        @JsonProperty("payment_id")
        private String paymentId;
        private String status;
        @JsonProperty("pay_params")
        private Map<String, Object> payParams;
    }

    public PaymentView createPayment(Long bookingId, Long tenantId, Long merchantId,
                                     BigDecimal amount, String channel) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_id", bookingId);
        body.put("tenant_id", tenantId);
        body.put("merchant_id", merchantId);
        body.put("amount", amount);
        body.put("channel", channel);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", String.valueOf(TenantContext.get()));

        ResponseEntity<ApiResult<PaymentView>> resp = restTemplate.exchange(
                baseUrl + "/api/v1/payments", HttpMethod.POST, new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<>() {
                });
        ApiResult<PaymentView> result = resp.getBody();
        if (result == null || result.getData() == null) {
            throw new IllegalStateException("payment-service 返回为空: bookingId=" + bookingId);
        }
        return result.getData();
    }
}
