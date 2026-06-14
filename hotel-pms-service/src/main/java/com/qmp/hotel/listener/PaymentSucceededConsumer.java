package com.qmp.hotel.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.hotel.event.PaymentSucceededPayload;
import com.qmp.hotel.service.HotelReservationService;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消费 {@code PaymentSucceeded}（topic={@code payment.payment-succeeded}）：
 * 若 order_id 对应酒店预订单，则确认连住预占并置预订单 CONFIRMED。
 *
 * <p>该主题被门票 order-service 与酒店 hotel-pms-service 同时订阅（不同 consumer_group），
 * 各自按 order_id 是否命中本域订单来认领，未命中则忽略（见 CLAUDE.md）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "payment.payment-succeeded",
        consumerGroup = PaymentSucceededConsumer.CONSUMER_GROUP,
        consumeMode = ConsumeMode.ORDERLY)
public class PaymentSucceededConsumer implements RocketMQListener<String> {

    public static final String CONSUMER_GROUP = "hotel-pms-payment-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final HotelReservationService reservationService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<PaymentSucceededPayload> envelope = parse(message);
        if (eventDedup.isProcessed(CONSUMER_GROUP, envelope.getEventId())) {
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            PaymentSucceededPayload payload = envelope.getPayload();
            reservationService.confirmPaid(payload.getOrderId(), payload.getPaymentId());
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
