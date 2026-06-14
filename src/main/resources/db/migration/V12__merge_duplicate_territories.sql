-- Merge case-insensitive duplicate active territories within each workspace.
-- Moved out of V10 after production had already applied that migration.

WITH ranked AS (
    SELECT territory_id,
           workspace_id,
           LOWER(location_name) AS location_key,
           ROW_NUMBER() OVER (
               PARTITION BY workspace_id, LOWER(location_name)
               ORDER BY territory_id
           ) AS rn
    FROM territories
    WHERE is_deleted = false
),
dupes AS (
    SELECT r.territory_id AS dupe_id,
           k.territory_id AS keep_id
    FROM ranked r
    JOIN ranked k
        ON k.workspace_id = r.workspace_id
       AND k.location_key = r.location_key
       AND k.rn = 1
    WHERE r.rn > 1
)
UPDATE customers c
SET territory_id = d.keep_id
FROM dupes d
WHERE c.territory_id = d.dupe_id;

-- Merged villages may collide on serial_number within the kept territory.
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

WITH ranked AS (
    SELECT territory_id,
           workspace_id,
           LOWER(location_name) AS location_key,
           ROW_NUMBER() OVER (
               PARTITION BY workspace_id, LOWER(location_name)
               ORDER BY territory_id
           ) AS rn
    FROM territories
    WHERE is_deleted = false
),
dupes AS (
    SELECT r.territory_id AS dupe_id,
           k.territory_id AS keep_id
    FROM ranked r
    JOIN ranked k
        ON k.workspace_id = r.workspace_id
       AND k.location_key = r.location_key
       AND k.rn = 1
    WHERE r.rn > 1
)
UPDATE territory_blocks tb
SET territory_id = d.keep_id
FROM dupes d
WHERE tb.territory_id = d.dupe_id;

DELETE FROM territories t
USING (
    SELECT territory_id
    FROM (
        SELECT territory_id,
               ROW_NUMBER() OVER (
                   PARTITION BY workspace_id, LOWER(location_name)
                   ORDER BY territory_id
               ) AS rn
        FROM territories
        WHERE is_deleted = false
    ) ranked
    WHERE rn > 1
) dupes
WHERE t.territory_id = dupes.territory_id;
