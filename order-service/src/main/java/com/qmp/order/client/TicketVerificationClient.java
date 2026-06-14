package com.qmp.order.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.order.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调用 ticket-verification-service 出票（09 文档七 {@code POST /api/v1/credentials}），幂等键 = order_item_id。
 * 透传 order_id/scenic_id/unit_price/payment_id（本服务出票时已掌握，见 ticket-verification CLAUDE.md v1 扩展）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketVerificationClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${order.client.ticket-verification-base-url:http://localhost:8086}")
    private String baseUrl;

    public void issueCredentials(OrderItem item, Long orderId, Long scenicId, String paymentId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_item_id", item.getOrderItemId());
        body.put("sku_id", item.getSkuId());
        body.put("sale_date", item.getSaleDate().toString());
        body.put("quantity", item.getQuantity());
        body.put("refund_policy_snapshot", parsePolicy(item.getRefundPolicySnapshot()));
        body.put("order_id", orderId);
        body.put("scenic_id", scenicId);
        body.put("unit_price", item.getUnitPrice());
        body.put("payment_id", paymentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", String.valueOf(TenantContext.get()));

        restTemplate.postForEntity(baseUrl + "/api/v1/credentials",
                new HttpEntity<>(body, headers), String.class);
        log.info("出票请求已发送: orderItemId={}", item.getOrderItemId());
    }

    /** 把库内存的退改规则 JSON 文本解析为对象再发送，避免被序列化成字符串。 */
    private JsonNode parsePolicy(String policyJson) {
        if (policyJson == null || policyJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(policyJson);
        } catch (Exception e) {
            log.warn("退改规则快照解析失败: {}", policyJson, e);
            return null;
        }
    }
}
