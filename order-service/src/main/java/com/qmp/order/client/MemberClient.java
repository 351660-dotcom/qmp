package com.qmp.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qmp.kernel.context.TenantContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 调用 member-service 查询会员身份（09 文档四 {@code GET /api/v1/members/{user_id}/status}）。
 * 无会员档案时返回 is_member=false（不报 404）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberClient {

    private final RestTemplate restTemplate;

    @Value("${order.client.member-base-url:http://localhost:8083}")
    private String baseUrl;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MemberView {
        @JsonProperty("is_member")
        private Boolean isMember;
    }

    public boolean isMember(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", String.valueOf(TenantContext.get()));
        ResponseEntity<ApiResult<MemberView>> resp = restTemplate.exchange(
                baseUrl + "/api/v1/members/" + userId + "/status", HttpMethod.GET,
                new HttpEntity<>(headers), new ParameterizedTypeReference<>() {
                });
        ApiResult<MemberView> body = resp.getBody();
        return body != null && body.getData() != null && Boolean.TRUE.equals(body.getData().getIsMember());
    }
}
