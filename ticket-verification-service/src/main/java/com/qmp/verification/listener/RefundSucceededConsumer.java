package com.qmp.verification.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.verification.client.InventoryClient;
import com.qmp.verification.entity.ProcessedEvent;
import com.qmp.verification.entity.TicketCredential;
import com.qmp.verification.event.RefundSucceededPayload;
import com.qmp.verification.mapper.ProcessedEventMapper;
import com.qmp.verification.service.CredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 消费 {@code RefundSucceeded}（topic={@code payment.refund-succeeded}）：
 * 据 credential_id 定位凭证 → 释放对应库存预占（reservation_id = order_item_id）→ 凭证置为终态 REFUNDED。
 *
 * <p>对应 09 文档八.2「RefundSucceeded → 若退款对应凭证在可释放窗口内，调用 inventory.ReleaseReservation」。
 * 申请退票时已校验可退窗口，v1 在此无条件释放（见 CLAUDE.md）。</p>
 *
 * <p>幂等：按 {@code (consumer_group, event_id)} 去重。处理顺序为
 * 「先释放库存（幂等）→ 再置终态（幂等）→ 最后写 processed_event」，
 * 任一步失败时不写去重记录，依赖 RocketMQ 重投后重跑（各步均幂等）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "payment.refund-succeeded",
        consumerGroup = RefundSucceededConsumer.CONSUMER_GROUP,
        consumeMode = ConsumeMode.ORDERLY)
public class RefundSucceededConsumer implements RocketMQListener<String> {

    public static final String CONSUMER_GROUP = "ticket-verification-refund-consumer";

    private final ObjectMapper objectMapper;
    private final ProcessedEventMapper processedEventMapper;
    private final CredentialService credentialService;
    private final InventoryClient inventoryClient;

    @Override
    public void onMessage(String message) {
        EventEnvelope<RefundSucceededPayload> envelope = parse(message);
        String eventId = envelope.getEventId();

        if (isProcessed(eventId)) {
            log.debug("RefundSucceeded 已处理，跳过: eventId={}", eventId);
            return;
        }

        RefundSucceededPayload payload = envelope.getPayload();
        Long tenantId = envelope.getTenantId();
        TenantContext.set(tenantId);
        try {
            TicketCredential credential = credentialService.findById(payload.getCredentialId());
            if (credential == null) {
                log.warn("RefundSucceeded: 凭证不存在 credentialId={}，仅写去重", payload.getCredentialId());
            } else {
                // 1) 释放库存（reservation_id = order_item_id），inventory 接口幂等
                inventoryClient.releaseReservation(tenantId, credential.getOrderItemId());
                // 2) 凭证置终态 REFUNDED，幂等
                credentialService.markRefunded(credential.getCredentialId());
            }
            // 3) 最后写去重，保证前序失败可重投重跑
            markProcessed(eventId, envelope.getEventType());
            log.info("RefundSucceeded 处理完成: credentialId={}, eventId={}", payload.getCredentialId(), eventId);
        } finally {
            TenantContext.clear();
        }
    }

    private EventEnvelope<RefundSucceededPayload> parse(String message) {
        try {
            return objectMapper.readValue(message, new TypeReference<EventEnvelope<RefundSucceededPayload>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("RefundSucceeded 事件解析失败: " + message, e);
        }
    }

    private boolean isProcessed(String eventId) {
        Long count = processedEventMapper.selectCount(new LambdaQueryWrapper<ProcessedEvent>()
                .eq(ProcessedEvent::getConsumerGroup, CONSUMER_GROUP)
                .eq(ProcessedEvent::getEventId, eventId));
        return count != null && count > 0;
    }

    private void markProcessed(String eventId, String eventType) {
        ProcessedEvent pe = new ProcessedEvent();
        pe.setConsumerGroup(CONSUMER_GROUP);
        pe.setEventId(eventId);
        pe.setEventType(eventType);
        pe.setProcessedAt(LocalDateTime.now());
        processedEventMapper.insert(pe);
    }
}
