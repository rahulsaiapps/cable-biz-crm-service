-- Full Supabase reset for CableCRM (no customer data to preserve).
-- Run in Supabase SQL Editor, then redeploy cable-crm-service on Render.
-- Flyway will recreate all tables from V2–V7 on the next deploy.

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

-- Optional: confirm schema is empty before redeploying Render
SELECT tablename FROM pg_tables WHERE schemaname = 'public';
