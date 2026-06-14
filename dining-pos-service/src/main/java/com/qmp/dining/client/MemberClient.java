package com.qmp.dining.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qmp.kernel.context.TenantContext;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调用 member-service 查询储值余额 / 扣减储值（12 文档 4.2 DeductWallet）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberClient {

    private final RestTemplate restTemplate;

    @Value("${dining.client.member-base-url:http://localhost:8083}")
    private String baseUrl;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WalletView {
        private BigDecimal balance;
    }

    public BigDecimal getWalletBalance(Long userId) {
        ResponseEntity<ApiResult<WalletView>> resp = restTemplate.exchange(
                baseUrl + "/api/v1/members/" + userId + "/wallet", HttpMethod.GET,
                new HttpEntity<>(tenantHeaders()), new ParameterizedTypeReference<>() {
                });
        ApiResult<WalletView> body = resp.getBody();
        return body != null && body.getData() != null && body.getData().getBalance() != null
                ? body.getData().getBalance() : BigDecimal.ZERO;
    }

    /** 扣减储值；source_ref 取台账号保证幂等。 */
    public void deductWallet(Long userId, BigDecimal amount, Long merchantId, String sourceRef) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("amount", amount);
        bodyMap.put("merchant_id", merchantId);
        bodyMap.put("source_ref", sourceRef);
        HttpHeaders headers = tenantHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(baseUrl + "/api/v1/members/" + userId + "/wallet/deduct",
                new HttpEntity<>(bodyMap, headers), String.class);
        log.info("会员储值抵扣: userId={}, amount={}, ref={}", userId, amount, sourceRef);
    }

    private HttpHeaders tenantHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", String.valueOf(TenantContext.get()));
        return headers;
    }
}
