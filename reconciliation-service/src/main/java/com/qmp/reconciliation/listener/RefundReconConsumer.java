package com.qmp.reconciliation.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.reconciliation.event.RefundSucceededPayload;
import com.qmp.reconciliation.service.ReconService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 对账：消费 {@code RefundSucceeded}，记一笔 OUT/REFUND。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "payment.refund-succeeded",
        consumerGroup = RefundReconConsumer.GROUP, consumeMode = ConsumeMode.ORDERLY)
public class RefundReconConsumer implements RocketMQListener<String> {

    public static final String GROUP = "reconciliation-refund-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final ReconService reconService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<RefundSucceededPayload> envelope;
        try {
            envelope = objectMapper.readValue(message,
                    new TypeReference<EventEnvelope<RefundSucceededPayload>>() {
                    });
        } catch (Exception e) {
            throw new IllegalStateException("RefundSucceeded 解析失败: " + message, e);
        }
        if (eventDedup.isProcessed(GROUP, envelope.getEventId())) {
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            RefundSucceededPayload p = envelope.getPayload();
            reconService.record(p.getMerchantId(), "REFUND", "OUT", p.getPaymentId(),
                    p.getAmount(), null, null);
            eventDedup.markProcessed(GROUP, envelope.getEventId(), envelope.getEventType());
        } finally {
            TenantContext.clear();
        }
    }
}
