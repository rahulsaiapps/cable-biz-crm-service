-- V5 may not drop standalone UNIQUE INDEXes (only pg_constraint rows).
-- Renumber per-territory serials if needed, then enforce (territory_id, serial_number).

-- Drop global serial_number unique constraints
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
          AND pg_get_constraintdef(c.oid) ~* 'serial_number'
          AND pg_get_constraintdef(c.oid) !~* 'territory_id'
    LOOP
        EXECUTE format('ALTER TABLE customers DROP CONSTRAINT IF EXISTS %I', constraint_name);
    END LOOP;
END $$;

-- Drop global serial_number unique indexes (Hibernate often creates these)
DO $$
DECLARE
    idx_name text;
BEGIN
    FOR idx_name IN
        SELECT indexname
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'customers'
          AND indexdef ILIKE '%unique%'
          AND indexdef ILIKE '%serial_number%'
          AND indexdef NOT ILIKE '%territory_id%'
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS %I', idx_name);
    END LOOP;
END $$;

DROP INDEX IF EXISTS uk_customers_serial_number;
DROP INDEX IF EXISTS idx_customers_serial_number_unique;
DROP INDEX IF EXISTS customers_serial_number_key;

-- Normalize serial numbers within each territory before composite unique index
WITH ranked AS (
    SELECT customer_id,
           ROW_NUMBER() OVER (
               PARTITION BY territory_id
               ORDER BY serial_number, customer_id
           ) AS new_serial
    FROM customers
    WHERE territory_id IS NOT NULL
)
UPDATE customers c
SET serial_number = r.new_serial
FROM ranked r
WHERE c.customer_id = r.customer_id
  AND c.serial_number IS DISTINCT FROM r.new_serial;

DROP INDEX IF EXISTS uq_customers_territory_serial;

CREATE UNIQUE INDEX uq_customers_territory_serial
    ON customers (territory_id, serial_number);
