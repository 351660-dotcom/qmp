package com.qmp.order.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.order.event.RefundSucceededPayload;
import com.qmp.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消费 {@code RefundSucceeded}（topic={@code payment_refund-succeeded}）：按 order_id 累加订单的
 * {@code refund_amount}（凭证置 REFUNDED 与库存释放由 ticket-verification 独立消费同一事件完成）。
 *
 * <p>幂等：{@code (consumer_group, event_id)} 去重；ORDERLY 与发布端 order_id 分区一致。
 * 处理顺序「业务（幂等）→ 写去重」，失败靠 MQ 重投。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "payment_refund-succeeded",
        consumerGroup = RefundSucceededConsumer.CONSUMER_GROUP,
        consumeMode = ConsumeMode.ORDERLY)
public class RefundSucceededConsumer implements RocketMQListener<String> {

    public static final String CONSUMER_GROUP = "order-service-refund-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final OrderService orderService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<RefundSucceededPayload> envelope = parse(message);
        if (eventDedup.isProcessed(CONSUMER_GROUP, envelope.getEventId())) {
            log.debug("RefundSucceeded 已处理，跳过: eventId={}", envelope.getEventId());
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            orderService.handleRefundSucceeded(envelope.getPayload());
            eventDedup.markProcessed(CONSUMER_GROUP, envelope.getEventId(), envelope.getEventType());
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
}
