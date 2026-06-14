package com.qmp.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件消费去重表（10 文档 9.1）。主键 {@code (consumer_group, event_id)}，无 tenant_id 列，
 * 已在 {@code inventory-kernel} 的 {@code TenantLineHandlerImpl.IGNORE_TABLES} 中排除租户拦截。
 */
@Data
@TableName("processed_event")
public class ProcessedEvent {

    private String consumerGroup;

    private String eventId;

    private String eventType;

    private LocalDateTime processedAt;
}
