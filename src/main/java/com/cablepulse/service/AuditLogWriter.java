package com.cablepulse.service;

import com.cablepulse.model.AuditEvent;
import com.cablepulse.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Async persistence delegate — separate bean so Spring AOP applies {@link Async}.
 */
@Component
class AuditLogWriter {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogWriter.class);

    private final AuditEventRepository auditEventRepository;

    AuditLogWriter(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Async
    void write(String actorUid, String actionType, String entityId, String details) {
        try {
            AuditEvent event = new AuditEvent(actorUid, actionType, entityId, details);
            auditEventRepository.save(event);
            logger.debug(
                    "Audit event persisted action={} entityId={} actor={}",
                    actionType,
                    entityId,
                    actorUid);
        } catch (Exception ex) {
            logger.error(
                    "Failed to persist audit event action={} entityId={} actor={}",
                    actionType,
                    entityId,
                    actorUid,
                    ex);
        }
    }
}
