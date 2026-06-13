-- Prevent duplicate team member emails within the same workspace.
CREATE UNIQUE INDEX IF NOT EXISTS uq_employees_workspace_email
    ON employees (workspace_id, LOWER(email))
    WHERE email IS NOT NULL AND TRIM(email) <> '';
