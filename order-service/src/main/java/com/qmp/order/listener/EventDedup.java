package com.qmp.order.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.order.entity.ProcessedEvent;
import com.qmp.order.mapper.ProcessedEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 事件消费去重（10 文档 9.1，按 {@code (consumer_group, event_id)}）。
 *
 * <p>约定调用顺序：消费开始先 {@link #isProcessed} 判断跳过；业务处理成功后再 {@link #markProcessed}。
 * 「最后写去重」保证业务步骤失败时不留去重记录，依赖 MQ 重投重跑（各业务步骤须幂等）。</p>
 */
@Component
@RequiredArgsConstructor
public class EventDedup {

    private final ProcessedEventMapper processedEventMapper;

    public boolean isProcessed(String consumerGroup, String eventId) {
        Long count = processedEventMapper.selectCount(new LambdaQueryWrapper<ProcessedEvent>()
                .eq(ProcessedEvent::getConsumerGroup, consumerGroup)
                .eq(ProcessedEvent::getEventId, eventId));
        return count != null && count > 0;
    }

    public void markProcessed(String consumerGroup, String eventId, String eventType) {
        ProcessedEvent pe = new ProcessedEvent();
        pe.setConsumerGroup(consumerGroup);
        pe.setEventId(eventId);
        pe.setEventType(eventType);
        pe.setProcessedAt(LocalDateTime.now());
        processedEventMapper.insert(pe);
    }
}
