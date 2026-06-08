package com.cablepulse.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "offline_sync_queue")
public class OfflineSyncQueue {

    @Id
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "idempotency_token", length = 100, nullable = false, unique = true)
    private String idempotencyToken;

    @Column(name = "payload_body", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payloadBody;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public OfflineSyncQueue() {}

    public OfflineSyncQueue(String eventId, String idempotencyToken, String payloadBody, String status, LocalDateTime processedAt) {
        this.eventId = eventId;
        this.idempotencyToken = idempotencyToken;
        this.payloadBody = payloadBody;
        this.status = status;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getIdempotencyToken() {
        return idempotencyToken;
    }

    public void setIdempotencyToken(String idempotencyToken) {
        this.idempotencyToken = idempotencyToken;
    }

    public String getPayloadBody() {
        return payloadBody;
    }

    public void setPayloadBody(String payloadBody) {
        this.payloadBody = payloadBody;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
