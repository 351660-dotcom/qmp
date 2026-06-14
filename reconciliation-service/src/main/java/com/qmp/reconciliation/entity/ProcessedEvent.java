package com.qmp.reconciliation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件消费去重表（10 文档 9.1）。无 tenant_id 列，已在 kernel TenantLineHandlerImpl 中排除。
 */
@Data
@TableName("processed_event")
public class ProcessedEvent {

    private String consumerGroup;

    private String eventId;

    private String eventType;

    private LocalDateTime processedAt;
}
