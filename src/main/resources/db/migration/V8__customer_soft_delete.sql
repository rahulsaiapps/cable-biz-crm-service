-- Soft-delete flag for customers (row kept; hidden from active lists).

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Allow reusing serial numbers after soft-delete within the same territory.
DROP INDEX IF EXISTS uq_customers_territory_serial;

CREATE UNIQUE INDEX uq_customers_territory_serial
    ON customers (territory_id, serial_number)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_customers_is_deleted ON customers (is_deleted);
