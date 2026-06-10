-- Flyway baseline marker.
-- Databases provisioned before Flyway adoption use spring.flyway.baseline-on-migrate=true
-- so this version is recorded without re-applying historical Hibernate DDL.

SELECT 1;
