#!/usr/bin/env bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username postgres <<'SQL'
-- INSTANCE DB
CREATE DATABASE syncturtle_instance;
CREATE USER instance_svc WITH ENCRYPTED PASSWORD 'instance_svc_dev';
GRANT ALL PRIVILEGES ON DATABASE syncturtle_instance TO instance_svc;

\connect syncturtle_instance

-- Ensure the service user can use and create in public
GRANT USAGE, CREATE ON SCHEMA public TO instance_svc;

ALTER DEFAULT PRIVILEGES FOR USER instance_svc IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO instance_svc;

ALTER DEFAULT PRIVILEGES FOR USER instance_svc IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO instance_svc;

-- USER DB
\connect postgres

CREATE DATABASE syncturtle_user;
CREATE USER user_svc WITH ENCRYPTED PASSWORD 'user_svc_dev';
GRANT ALL PRIVILEGES ON DATABASE syncturtle_user TO user_svc;

\connect syncturtle_user
GRANT USAGE, CREATE ON SCHEMA public TO user_svc;

ALTER DEFAULT PRIVILEGES FOR USER user_svc IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO user_svc;

ALTER DEFAULT PRIVILEGES FOR USER user_svc IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO user_svc;
SQL
