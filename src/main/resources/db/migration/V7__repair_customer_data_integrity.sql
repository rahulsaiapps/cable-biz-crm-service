-- Repair legacy customer rows that block registration (orphan territory links,
-- duplicate per-territory serials). Safe to run repeatedly.

-- Remove customers pointing at a territory_id that no longer exists.
DELETE FROM customers c
WHERE c.territory_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM territories t WHERE t.territory_id = c.territory_id
  );

-- Renumber serials sequentially within each territory (fixes duplicate pairs).
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
