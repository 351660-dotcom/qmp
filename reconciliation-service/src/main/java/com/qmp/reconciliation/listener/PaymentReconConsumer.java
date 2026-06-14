package com.qmp.reconciliation.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.reconciliation.event.PaymentSucceededPayload;
import com.qmp.reconciliation.service.ReconService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 对账：消费 {@code PaymentSucceeded}，记一笔 IN/PAYMENT（覆盖门票/酒店/演出等支付型业态）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "payment.payment-succeeded",
        consumerGroup = PaymentReconConsumer.GROUP, consumeMode = ConsumeMode.ORDERLY)
public class PaymentReconConsumer implements RocketMQListener<String> {

    public static final String GROUP = "reconciliation-payment-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final ReconService reconService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<PaymentSucceededPayload> envelope;
        try {
            envelope = objectMapper.readValue(message,
                    new TypeReference<EventEnvelope<PaymentSucceededPayload>>() {
                    });
        } catch (Exception e) {
            throw new IllegalStateException("PaymentSucceeded 解析失败: " + message, e);
        }
        if (eventDedup.isProcessed(GROUP, envelope.getEventId())) {
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            PaymentSucceededPayload p = envelope.getPayload();
            LocalDateTime occurredAt = p.getPaidAt() != null
                    ? LocalDateTime.ofInstant(p.getPaidAt(), ZoneId.systemDefault()) : null;
            reconService.record(p.getMerchantId(), "PAYMENT", "IN", p.getPaymentId(),
                    p.getAmount(), p.getChannel(), occurredAt);
            eventDedup.markProcessed(GROUP, envelope.getEventId(), envelope.getEventType());
        } finally {
            TenantContext.clear();
        }
    }
}
