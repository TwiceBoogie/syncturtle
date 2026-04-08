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

-- WORKSPACE DB
\connect postgres

CREATE DATABASE syncturtle_workspace;
CREATE USER workspace_svc WITH ENCRYPTED PASSWORD 'workspace_svc_dev';
GRANT ALL PRIVILEGES ON DATABASE syncturtle_workspace TO workspace_svc;

\connect syncturtle_workspace
GRANT USAGE, CREATE ON SCHEMA public TO workspace_svc;

ALTER DEFAULT PRIVILEGES FOR USER workspace_svc IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO workspace_svc;

ALTER DEFAULT PRIVILEGES FOR USER workspace_svc IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO workspace_svc;

-- EMAIL DB
\connect postgres

CREATE DATABASE syncturtle_email;
CREATE USER email_svc WITH ENCRYPTED PASSWORD 'email_svc_dev';
GRANT ALL PRIVILEGES ON DATABASE syncturtle_email TO email_svc;

\connect syncturtle_email
GRANT USAGE, CREATE ON SCHEMA public TO email_svc;

ALTER DEFAULT PRIVILEGES FOR USER email_svc IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO email_svc;

ALTER DEFAULT PRIVILEGES FOR USER email_svc IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO email_svc;
SQL
