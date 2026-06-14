package com.qmp.order.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.order.event.PaymentSucceededPayload;
import com.qmp.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消费 {@code PaymentSucceeded}（topic={@code payment_payment-succeeded}）：
 * 逐 item 确认预占 + 出票，订单置 PAID（09 文档 8.2）。
 *
 * <p>幂等：{@code (consumer_group, event_id)} 去重；ORDERLY 与发布端 order_id 分区一致。
 * 处理顺序「业务（幂等）→ 写去重」，失败靠 MQ 重投。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "payment_payment-succeeded",
        consumerGroup = PaymentSucceededConsumer.CONSUMER_GROUP,
        consumeMode = ConsumeMode.ORDERLY)
public class PaymentSucceededConsumer implements RocketMQListener<String> {

    public static final String CONSUMER_GROUP = "order-service-payment-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final OrderService orderService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<PaymentSucceededPayload> envelope = parse(message);
        if (eventDedup.isProcessed(CONSUMER_GROUP, envelope.getEventId())) {
            log.debug("PaymentSucceeded 已处理，跳过: eventId={}", envelope.getEventId());
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            orderService.handlePaymentSucceeded(envelope.getPayload());
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
