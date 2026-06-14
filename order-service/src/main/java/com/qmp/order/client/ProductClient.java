package com.qmp.order.client;

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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 调用 product-service 查询票种（09 文档二 {@code GET /api/v1/skus/{sku_id}}）。
 */
@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${order.client.product-base-url:http://localhost:8081}")
    private String baseUrl;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkuView {
        @JsonProperty("sku_id")
        private Long skuId;
        private String status;
        @JsonProperty("scenic_id")
        private Long scenicId;
        @JsonProperty("merchant_id")
        private Long merchantId;
    }

    public SkuView getSku(Long skuId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", String.valueOf(TenantContext.get()));
        ResponseEntity<ApiResult<SkuView>> resp = restTemplate.exchange(
                baseUrl + "/api/v1/skus/" + skuId, HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });
        ApiResult<SkuView> body = resp.getBody();
        if (body == null || body.getData() == null) {
            throw new IllegalStateException("product-service 返回为空: skuId=" + skuId);
        }
        return body.getData();
    }
}
