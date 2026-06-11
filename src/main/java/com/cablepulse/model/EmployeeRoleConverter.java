package com.cablepulse.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link EmployeeRole} as {@code OWNER} / {@code COLLECTION_BOY} while
 * accepting legacy rows that were saved with a {@code ROLE_} prefix.
 */
@Converter(autoApply = true)
public class EmployeeRoleConverter implements AttributeConverter<EmployeeRole, String> {

    @Override
    public String convertToDatabaseColumn(EmployeeRole role) {
        return role == null ? null : role.name();
    }

    @Override
    public EmployeeRole convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return EmployeeRole.COLLECTION_BOY;
        }
        String normalized = dbValue.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return EmployeeRole.valueOf(normalized);
    }
}
