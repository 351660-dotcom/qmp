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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 调用 pricing-service 查询价格（09 文档三 {@code GET /api/v1/price}）。
 * 该 sku 当日未配置门市价时 pricing 返回 404，RestTemplate 抛异常向上透出（订单创建应失败而非默认 0）。
 */
@Component
@RequiredArgsConstructor
public class PricingClient {

    private final RestTemplate restTemplate;

    @Value("${order.client.pricing-base-url:http://localhost:8082}")
    private String baseUrl;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceView {
        @JsonProperty("price_type")
        private String priceType;
        private BigDecimal price;
    }

    public PriceView getPrice(Long skuId, LocalDate saleDate, boolean isMember) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/v1/price")
                .queryParam("sku_id", skuId)
                .queryParam("sale_date", saleDate)
                .queryParam("is_member", isMember)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", String.valueOf(TenantContext.get()));
        ResponseEntity<ApiResult<PriceView>> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });
        ApiResult<PriceView> body = resp.getBody();
        if (body == null || body.getData() == null) {
            throw new IllegalStateException("pricing-service 返回为空: skuId=" + skuId);
        }
        return body.getData();
    }
}
