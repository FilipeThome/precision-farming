-- First volume init only.
CREATE DATABASE auth_db;
CREATE DATABASE farm_db;
CREATE DATABASE asset_db;
CREATE DATABASE weather_db;
CREATE DATABASE operation_db;
CREATE DATABASE inventory_db;
CREATE DATABASE alert_db;
CREATE DATABASE ai_db;
CREATE DATABASE notification_db;
CREATE DATABASE file_db;
CREATE DATABASE reporting_db;
CREATE DATABASE sync_db;
CREATE DATABASE integration_db;
CREATE DATABASE agronomy_db;
CREATE DATABASE irrigation_db;
CREATE DATABASE harvest_db;
CREATE DATABASE finance_db;
CREATE DATABASE compliance_db;

\connect farm_db
CREATE EXTENSION IF NOT EXISTS postgis;
