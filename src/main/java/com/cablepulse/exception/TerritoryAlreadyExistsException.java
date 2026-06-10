package com.cablepulse.exception;

import com.cablepulse.model.Territory;

public class TerritoryAlreadyExistsException extends RuntimeException {

    private final Territory existing;

    public TerritoryAlreadyExistsException(Territory existing) {
        super("Territory already exists");
        this.existing = existing;
    }

    public Territory getExisting() {
        return existing;
    }
}
