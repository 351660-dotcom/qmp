package com.qmp.reconciliation.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.reconciliation.event.WalletConsumedPayload;
import com.qmp.reconciliation.service.ReconService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 对账：消费 {@code WalletConsumed}（会员储值消费，如餐饮结账抵扣），记一笔 IN/WALLET。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "member.wallet-consumed",
        consumerGroup = WalletReconConsumer.GROUP, consumeMode = ConsumeMode.ORDERLY)
public class WalletReconConsumer implements RocketMQListener<String> {

    public static final String GROUP = "reconciliation-wallet-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final ReconService reconService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<WalletConsumedPayload> envelope;
        try {
            envelope = objectMapper.readValue(message,
                    new TypeReference<EventEnvelope<WalletConsumedPayload>>() {
                    });
        } catch (Exception e) {
            throw new IllegalStateException("WalletConsumed 解析失败: " + message, e);
        }
        if (eventDedup.isProcessed(GROUP, envelope.getEventId())) {
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            WalletConsumedPayload p = envelope.getPayload();
            reconService.record(p.getMerchantId(), "WALLET", "IN", p.getSourceRef(),
                    p.getAmount(), "WALLET", null);
            eventDedup.markProcessed(GROUP, envelope.getEventId(), envelope.getEventType());
        } finally {
            TenantContext.clear();
        }
    }
}
