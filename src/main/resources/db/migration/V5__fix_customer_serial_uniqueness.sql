-- Customer serial numbers are unique per territory, not globally.
-- Legacy Hibernate / manual schemas may have created a global UNIQUE on serial_number,
-- which causes every new customer after the first to fail with HTTP 409.

DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        WHERE t.relname = 'customers'
          AND c.contype = 'u'
          AND pg_get_constraintdef(c.oid) ~ 'serial_number'
          AND pg_get_constraintdef(c.oid) !~ 'territory_id'
    LOOP
        EXECUTE format('ALTER TABLE customers DROP CONSTRAINT IF EXISTS %I', constraint_name);
    END LOOP;
END $$;

DROP INDEX IF EXISTS uk_customers_serial_number;
DROP INDEX IF EXISTS idx_customers_serial_number_unique;

CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_territory_serial
    ON customers (territory_id, serial_number);
