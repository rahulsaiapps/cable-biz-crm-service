package com.cablepulse.security;

import org.springframework.security.access.AccessDeniedException;

/**
 * Request-scoped tenant (workspace) identity derived from the authenticated employee.
 */
public final class TenantContext {

    private static final ThreadLocal<String> WORKSPACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> BUSINESS_NAME = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String workspaceId, String businessName) {
        WORKSPACE_ID.set(workspaceId);
        BUSINESS_NAME.set(businessName);
    }

    public static void clear() {
        WORKSPACE_ID.remove();
        BUSINESS_NAME.remove();
    }

    public static String workspaceId() {
        return WORKSPACE_ID.get();
    }

    public static String businessName() {
        return BUSINESS_NAME.get();
    }

    public static String requireWorkspaceId() {
        String workspaceId = WORKSPACE_ID.get();
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new AccessDeniedException("Workspace access denied");
        }
        return workspaceId;
    }
}
