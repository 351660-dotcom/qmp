package com.qmp.kernel.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * 全平台统一 MQ 事件 Envelope（见 09 文档 1.3）。
 *
 * <pre>{@code
 * {
 *   "event_id": "uuid-v4",
 *   "event_type": "PaymentSucceeded",
 *   "schema_version": "1.0",
 *   "tenant_id": 1001,
 *   "occurred_at": "2026-07-01T10:00:00Z",
 *   "payload": { }
 * }
 * }</pre>
 *
 * Topic 命名：{@code {domain}.{event-name-kebab}}，如 {@code payment_payment-succeeded}。
 * 消费方按 {@code event_id} 写入各自的 {@code processed_event} 表去重。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope<T> {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("schema_version")
    private String schemaVersion;

    @JsonProperty("tenant_id")
    private Long tenantId;

    @JsonProperty("occurred_at")
    private Instant occurredAt;

    private T payload;

    public static <T> EventEnvelope<T> of(String eventType, Long tenantId, T payload) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .schemaVersion("1.0")
                .tenantId(tenantId)
                .occurredAt(Instant.now())
                .payload(payload)
                .build();
    }
}
