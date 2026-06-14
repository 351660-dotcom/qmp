package com.qmp.verification.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 同步调用 inventory-service 释放预占（09 文档五
 * {@code POST /api/v1/inventory/reservations/{reservation_id}/release}）。
 * reservation_id 约定 = order_item_id；接口幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${verification.client.inventory-base-url:http://localhost:8084}")
    private String inventoryBaseUrl;

    public void releaseReservation(Long tenantId, String reservationId) {
        String url = inventoryBaseUrl + "/api/v1/inventory/reservations/" + reservationId + "/release";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", String.valueOf(tenantId));

        restTemplate.postForEntity(url, new HttpEntity<>(headers), Void.class);
        log.info("inventory-service 释放预占成功: reservationId={}", reservationId);
    }
}
