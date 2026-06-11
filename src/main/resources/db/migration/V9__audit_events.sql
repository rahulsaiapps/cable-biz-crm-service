-- Forensic audit trail for sensitive owner-level mutations.

CREATE TABLE IF NOT EXISTS audit_events (
    id              BIGSERIAL PRIMARY KEY,
    actor_uid       VARCHAR(255) NOT NULL,
    action_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(255) NOT NULL,
    details         TEXT,
    recorded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_events_actor_uid
    ON audit_events (actor_uid);

CREATE INDEX IF NOT EXISTS idx_audit_events_action_type
    ON audit_events (action_type);

CREATE INDEX IF NOT EXISTS idx_audit_events_entity_id
    ON audit_events (entity_id);

CREATE INDEX IF NOT EXISTS idx_audit_events_recorded_at
    ON audit_events (recorded_at DESC);
