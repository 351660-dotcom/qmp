package com.qmp.hotel.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.hotel.entity.ProcessedEvent;
import com.qmp.hotel.mapper.ProcessedEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 事件消费去重（10 文档 9.1，按 {@code (consumer_group, event_id)}）。
 * 约定：消费开始先 {@link #isProcessed} 跳过；业务成功后再 {@link #markProcessed}（最后写去重）。
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
