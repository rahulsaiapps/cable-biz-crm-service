-- Legacy test rows stored Spring-style names (ROLE_OWNER) instead of enum names (OWNER).
UPDATE employees
SET role = 'OWNER'
WHERE role IN ('ROLE_OWNER', 'role_owner');

UPDATE employees
SET role = 'COLLECTION_BOY'
WHERE role IN ('ROLE_COLLECTION_BOY', 'role_collection_boy');
