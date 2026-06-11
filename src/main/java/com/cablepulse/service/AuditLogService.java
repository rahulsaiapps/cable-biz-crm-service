package com.cablepulse.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Forensic audit trail for owner-protected mutations.
 * Captures the authenticated Firebase UID on the request thread, then persists asynchronously.
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    public static final String DELETE_TERRITORY = "DELETE_TERRITORY";
    public static final String RECORD_PAYMENT = "RECORD_PAYMENT";
    public static final String UPDATE_SUBSCRIPTION = "UPDATE_SUBSCRIPTION";
    public static final String CREATE_EXPENSE = "CREATE_EXPENSE";

    private final AuditLogWriter auditLogWriter;

    public AuditLogService(AuditLogWriter auditLogWriter) {
        this.auditLogWriter = auditLogWriter;
    }

    /**
     * Records an audit event without blocking the caller.
     *
     * @param actionType stable action code (e.g. {@link #DELETE_TERRITORY})
     * @param entityId   primary key of the affected row
     * @param details    optional JSON metadata snapshot
     */
    public void log(String actionType, String entityId, String details) {
        String actorUid = resolveActorUid();
        auditLogWriter.write(actorUid, actionType, entityId, details);
    }

    private String resolveActorUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        String name = authentication.getName();
        return name != null && !name.isBlank() ? name : "anonymous";
    }
}
