package com.qmp.performance.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.performance.event.PaymentSucceededPayload;
import com.qmp.performance.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消费 {@code PaymentSucceeded}：若 order_id 命中演出/游船/游乐预订单则确认预占（与门票/酒店共用主题，按 id 认领）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "payment_payment-succeeded",
        consumerGroup = PaymentSucceededConsumer.CONSUMER_GROUP,
        consumeMode = ConsumeMode.ORDERLY)
public class PaymentSucceededConsumer implements RocketMQListener<String> {

    public static final String CONSUMER_GROUP = "performance-payment-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final BookingService bookingService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<PaymentSucceededPayload> envelope = parse(message);
        if (eventDedup.isProcessed(CONSUMER_GROUP, envelope.getEventId())) {
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            PaymentSucceededPayload payload = envelope.getPayload();
            bookingService.confirmPaid(payload.getOrderId(), payload.getPaymentId());
            eventDedup.markProcessed(CONSUMER_GROUP, envelope.getEventId(), envelope.getEventType());
        } finally {
            TenantContext.clear();
        }
    }

    private EventEnvelope<PaymentSucceededPayload> parse(String message) {
        try {
            return objectMapper.readValue(message, new TypeReference<EventEnvelope<PaymentSucceededPayload>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("PaymentSucceeded 事件解析失败: " + message, e);
        }
    }
}
