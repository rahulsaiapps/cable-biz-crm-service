-- Multi-tenant workspaces: isolate CRM data per business operator.

CREATE TABLE IF NOT EXISTS workspaces (
    workspace_id    VARCHAR(255) PRIMARY KEY,
    business_name   VARCHAR(255) NOT NULL,
    created_by_uid  VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO workspaces (workspace_id, business_name, created_by_uid, created_at)
VALUES ('ws_legacy_default', 'Legacy Workspace', NULL, NOW())
ON CONFLICT (workspace_id) DO NOTHING;

-- employees
ALTER TABLE employees ADD COLUMN IF NOT EXISTS workspace_id VARCHAR(255);
UPDATE employees SET workspace_id = 'ws_legacy_default' WHERE workspace_id IS NULL;
ALTER TABLE employees ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE employees
    ADD CONSTRAINT fk_employees_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces (workspace_id);

-- territories
ALTER TABLE territories ADD COLUMN IF NOT EXISTS workspace_id VARCHAR(255);
UPDATE territories SET workspace_id = 'ws_legacy_default' WHERE workspace_id IS NULL;
ALTER TABLE territories ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE territories
    ADD CONSTRAINT fk_territories_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces (workspace_id);

-- connection_providers: per-workspace names
ALTER TABLE connection_providers ADD COLUMN IF NOT EXISTS workspace_id VARCHAR(255);
UPDATE connection_providers SET workspace_id = 'ws_legacy_default' WHERE workspace_id IS NULL;
ALTER TABLE connection_providers ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE connection_providers
    ADD CONSTRAINT fk_connection_providers_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces (workspace_id);
ALTER TABLE connection_providers DROP CONSTRAINT IF EXISTS connection_providers_name_key;

-- Legacy rows may differ only by casing (e.g. "Jio" vs "jio") while the old
-- name constraint was case-sensitive. Keep the lowest id and re-point plans.
WITH ranked AS (
    SELECT id,
           workspace_id,
           LOWER(name) AS name_key,
           ROW_NUMBER() OVER (
               PARTITION BY workspace_id, LOWER(name)
               ORDER BY id
           ) AS rn
    FROM connection_providers
),
dupes AS (
    SELECT r.id AS dupe_id,
           k.id AS keep_id
    FROM ranked r
    JOIN ranked k
        ON k.workspace_id = r.workspace_id
       AND k.name_key = r.name_key
       AND k.rn = 1
    WHERE r.rn > 1
)
UPDATE global_plans gp
SET provider_id = d.keep_id
FROM dupes d
WHERE gp.provider_id = d.dupe_id;

DELETE FROM connection_providers cp
WHERE cp.id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY workspace_id, LOWER(name)
                   ORDER BY id
               ) AS rn
        FROM connection_providers
    ) ranked
    WHERE rn > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_connection_providers_workspace_name
    ON connection_providers (workspace_id, LOWER(name));

-- global_plans
ALTER TABLE global_plans ADD COLUMN IF NOT EXISTS workspace_id VARCHAR(255);
UPDATE global_plans SET workspace_id = 'ws_legacy_default' WHERE workspace_id IS NULL;
ALTER TABLE global_plans ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE global_plans
    ADD CONSTRAINT fk_global_plans_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces (workspace_id);

-- customers
ALTER TABLE customers ADD COLUMN IF NOT EXISTS workspace_id VARCHAR(255);
UPDATE customers c
SET workspace_id = t.workspace_id
FROM territories t
WHERE c.territory_id = t.territory_id
  AND c.workspace_id IS NULL;
UPDATE customers SET workspace_id = 'ws_legacy_default' WHERE workspace_id IS NULL;
ALTER TABLE customers ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE customers
    ADD CONSTRAINT fk_customers_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces (workspace_id);

-- daily_expenses
ALTER TABLE daily_expenses ADD COLUMN IF NOT EXISTS workspace_id VARCHAR(255);
UPDATE daily_expenses SET workspace_id = 'ws_legacy_default' WHERE workspace_id IS NULL;
ALTER TABLE daily_expenses ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE daily_expenses
    ADD CONSTRAINT fk_daily_expenses_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces (workspace_id);

-- isp_vendor_settlements
ALTER TABLE isp_vendor_settlements ADD COLUMN IF NOT EXISTS workspace_id VARCHAR(255);
UPDATE isp_vendor_settlements SET workspace_id = 'ws_legacy_default' WHERE workspace_id IS NULL;
ALTER TABLE isp_vendor_settlements ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE isp_vendor_settlements
    ADD CONSTRAINT fk_isp_settlements_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces (workspace_id);

CREATE INDEX IF NOT EXISTS idx_employees_workspace_id ON employees (workspace_id);
CREATE INDEX IF NOT EXISTS idx_territories_workspace_id ON territories (workspace_id);
CREATE INDEX IF NOT EXISTS idx_customers_workspace_id ON customers (workspace_id);
CREATE INDEX IF NOT EXISTS idx_global_plans_workspace_id ON global_plans (workspace_id);
CREATE INDEX IF NOT EXISTS idx_connection_providers_workspace_id ON connection_providers (workspace_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_territories_workspace_location_active
    ON territories (workspace_id, LOWER(location_name))
    WHERE is_deleted = false;
