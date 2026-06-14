package com.qmp.order.client;

import com.qmp.kernel.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调用 inventory-service 预占/确认/释放（09 文档五）。
 * reservation_id 取 order_item_id。createReservation 库存不足时 inventory 返回 409，异常向上透出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${order.client.inventory-base-url:http://localhost:8084}")
    private String baseUrl;

    public void createReservation(String reservationId, Long skuId, LocalDate saleDate,
                                  Long timeSlotId, Integer quantity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reservation_id", reservationId);
        body.put("sku_id", skuId);
        body.put("sale_date", saleDate.toString());
        body.put("time_slot_id", timeSlotId);
        body.put("quantity", quantity);

        HttpHeaders headers = jsonHeaders();
        restTemplate.postForEntity(baseUrl + "/api/v1/inventory/reservations",
                new HttpEntity<>(body, headers), String.class);
    }

    public void confirmReservation(String reservationId) {
        restTemplate.postForEntity(
                baseUrl + "/api/v1/inventory/reservations/" + reservationId + "/confirm",
                new HttpEntity<>(jsonHeaders()), String.class);
    }

    public void releaseReservation(String reservationId) {
        restTemplate.postForEntity(
                baseUrl + "/api/v1/inventory/reservations/" + reservationId + "/release",
                new HttpEntity<>(jsonHeaders()), String.class);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", String.valueOf(TenantContext.get()));
        return headers;
    }
}
