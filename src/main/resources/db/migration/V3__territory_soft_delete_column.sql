-- Legacy databases created before V2 may lack the soft-delete flag.
ALTER TABLE territories
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
