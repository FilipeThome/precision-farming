-- Run against an existing PostGIS instance when the volume was created before the full-spec DBs existed.
-- Example: psql -h localhost -U precision -d postgres -f scripts/ensure-new-dbs.sql
CREATE DATABASE agronomy_db;
CREATE DATABASE irrigation_db;
CREATE DATABASE harvest_db;
CREATE DATABASE finance_db;
CREATE DATABASE compliance_db;
