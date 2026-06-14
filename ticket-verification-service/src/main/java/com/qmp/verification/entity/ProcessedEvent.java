package com.qmp.verification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件消费去重表（10 文档 9.1）。主键 {@code (consumer_group, event_id)}，无 tenant_id 列，
 * 已在 {@code inventory-kernel} 的 {@code TenantLineHandlerImpl.IGNORE_TABLES} 中排除租户拦截。
 *
 * <p>本服务每个 consumer 独立一份去重逻辑（共用同表，按 consumer_group 区分）。</p>
 */
@Data
@TableName("processed_event")
public class ProcessedEvent {

    private String consumerGroup;

    private String eventId;

    private String eventType;

    private LocalDateTime processedAt;
}
