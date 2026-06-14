package com.qmp.member.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.member.event.OrderPaidPayload;
import com.qmp.member.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 消费 {@code OrderPaid}（topic={@code order_order-paid}）：按消费金额发放积分（13 文档 1.3/3.4）。
 *
 * <p>v1 积分规则：1 元 = 1 积分（向下取整）。幂等：积分账本 {@code (source_ref=ORDER:{id}, EARN)} 唯一，
 * 叠加 processed_event 去重。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "order_order-paid",
        consumerGroup = OrderPaidConsumer.CONSUMER_GROUP,
        consumeMode = ConsumeMode.ORDERLY)
public class OrderPaidConsumer implements RocketMQListener<String> {

    public static final String CONSUMER_GROUP = "member-service-order-paid-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final PointService pointService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<OrderPaidPayload> envelope = parse(message);
        if (eventDedup.isProcessed(CONSUMER_GROUP, envelope.getEventId())) {
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            OrderPaidPayload payload = envelope.getPayload();
            int points = payload.getTotalAmount() != null
                    ? payload.getTotalAmount().setScale(0, java.math.RoundingMode.DOWN).intValueExact()
                    : 0;
            pointService.earn(payload.getUserId(), points, payload.getMerchantId(), "ORDER:" + payload.getOrderId());
            eventDedup.markProcessed(CONSUMER_GROUP, envelope.getEventId(), envelope.getEventType());
        } finally {
            TenantContext.clear();
        }
    }

    private EventEnvelope<OrderPaidPayload> parse(String message) {
        try {
            return objectMapper.readValue(message, new TypeReference<EventEnvelope<OrderPaidPayload>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("OrderPaid 事件解析失败: " + message, e);
        }
    }
}
