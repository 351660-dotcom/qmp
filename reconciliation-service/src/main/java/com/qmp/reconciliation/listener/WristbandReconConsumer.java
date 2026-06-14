package com.qmp.reconciliation.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.reconciliation.event.WristbandConsumedPayload;
import com.qmp.reconciliation.service.ReconService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 对账：消费 {@code WristbandConsumed}（手牌二次消费），记一笔 IN/WRISTBAND。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "performance.wristband-consumed",
        consumerGroup = WristbandReconConsumer.GROUP, consumeMode = ConsumeMode.ORDERLY)
public class WristbandReconConsumer implements RocketMQListener<String> {

    public static final String GROUP = "reconciliation-wristband-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final ReconService reconService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<WristbandConsumedPayload> envelope;
        try {
            envelope = objectMapper.readValue(message,
                    new TypeReference<EventEnvelope<WristbandConsumedPayload>>() {
                    });
        } catch (Exception e) {
            throw new IllegalStateException("WristbandConsumed 解析失败: " + message, e);
        }
        if (eventDedup.isProcessed(GROUP, envelope.getEventId())) {
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            WristbandConsumedPayload p = envelope.getPayload();
            reconService.record(p.getMerchantId(), "WRISTBAND", "IN", p.getSourceRef(),
                    p.getAmount(), "WRISTBAND", null);
            eventDedup.markProcessed(GROUP, envelope.getEventId(), envelope.getEventType());
        } finally {
            TenantContext.clear();
        }
    }
}
