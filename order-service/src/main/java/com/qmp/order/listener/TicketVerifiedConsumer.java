package com.qmp.order.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.order.event.TicketVerifiedPayload;
import com.qmp.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消费 {@code TicketVerified}（topic={@code ticket-verification_ticket-verified}）：
 * 更新对应 OrderItem 凭证聚合，若订单全部核销则置 CLOSED（09 文档 8.2）。
 *
 * <p>幂等：{@code (consumer_group, event_id)} 去重保证每个核销事件只自增一次；
 * ORDERLY 与发布端 order_id 分区一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "ticket-verification_ticket-verified",
        consumerGroup = TicketVerifiedConsumer.CONSUMER_GROUP,
        consumeMode = ConsumeMode.ORDERLY)
public class TicketVerifiedConsumer implements RocketMQListener<String> {

    public static final String CONSUMER_GROUP = "order-service-ticket-verified-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final OrderService orderService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<TicketVerifiedPayload> envelope = parse(message);
        if (eventDedup.isProcessed(CONSUMER_GROUP, envelope.getEventId())) {
            log.debug("TicketVerified 已处理，跳过: eventId={}", envelope.getEventId());
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            orderService.handleTicketVerified(envelope.getPayload());
            eventDedup.markProcessed(CONSUMER_GROUP, envelope.getEventId(), envelope.getEventType());
        } finally {
            TenantContext.clear();
        }
    }

    private EventEnvelope<TicketVerifiedPayload> parse(String message) {
        try {
            return objectMapper.readValue(message, new TypeReference<EventEnvelope<TicketVerifiedPayload>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("TicketVerified 事件解析失败: " + message, e);
        }
    }
}
