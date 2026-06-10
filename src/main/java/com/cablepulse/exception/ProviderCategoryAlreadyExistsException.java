package com.cablepulse.exception;

import com.cablepulse.model.ConnectionProvider;

public class ProviderCategoryAlreadyExistsException extends RuntimeException {

    private final ConnectionProvider existing;

    public ProviderCategoryAlreadyExistsException(ConnectionProvider existing) {
        super("Provider category already exists");
        this.existing = existing;
    }

    public ConnectionProvider getExisting() {
        return existing;
    }
}
