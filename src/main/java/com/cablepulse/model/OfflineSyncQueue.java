package com.cablepulse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "offline_sync_queue")
public class OfflineSyncQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_id")
    private Long syncId;

    @Column(name = "idempotency_token", nullable = false, unique = true)
    private UUID idempotencyToken;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public OfflineSyncQueue() {}

    public OfflineSyncQueue(UUID idempotencyToken, String status, String eventId, LocalDateTime processedAt) {
        this.idempotencyToken = idempotencyToken;
        this.status = status;
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public Long getSyncId() {
        return syncId;
    }

    public void setSyncId(Long syncId) {
        this.syncId = syncId;
    }

    public UUID getIdempotencyToken() {
        return idempotencyToken;
    }

    public void setIdempotencyToken(UUID idempotencyToken) {
        this.idempotencyToken = idempotencyToken;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
