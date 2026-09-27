-- Idempotent. Safe on a fresh volume (init-postgis.sql already created these)
-- and on an older volume that predates the domain databases.
-- psql: psql -h localhost -U precision -d postgres -f scripts/ensure-new-dbs.sql
SELECT format('CREATE DATABASE %I', datname)
FROM (
  VALUES
    ('agronomy_db'),
    ('irrigation_db'),
    ('harvest_db'),
    ('finance_db'),
    ('compliance_db')
) AS needed(datname)
WHERE NOT EXISTS (
  SELECT 1 FROM pg_database WHERE datname = needed.datname
)
\gexec
