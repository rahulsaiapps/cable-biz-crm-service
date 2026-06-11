package com.cablepulse.util;

/**
 * Redacts personally identifiable information for non-privileged API responses and logs.
 */
public final class PiiMaskingUtil {

    private PiiMaskingUtil() {}

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    public static String redactIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "redacted";
        }
        String trimmed = identifier.trim();
        if (trimmed.length() <= 8) {
            return trimmed.substring(0, Math.min(4, trimmed.length())) + "…";
        }
        return trimmed.substring(0, 4) + "…" + trimmed.substring(trimmed.length() - 4);
    }
}
