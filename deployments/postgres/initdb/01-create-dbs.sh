#!/usr/bin/env bash

set -euo pipefail

: "${INSTANCE_DB_PASSWORD:?INSTANCE_DB_PASSWORD is required}"
: "${INSTANCE_MIGRATOR_DB_PASSWORD:?INSTANCE_MIGRATOR_DB_PASSWORD is required}"
: "${USER_DB_PASSWORD:?USER_DB_PASSWORD is required}"
: "${USER_MIGRATOR_DB_PASSWORD:?USER_MIGRATOR_DB_PASSWORD is required}"
: "${WORKSPACE_DB_PASSWORD:?WORKSPACE_DB_PASSWORD is required}"
: "${WORKSPACE_MIGRATOR_DB_PASSWORD:?WORKSPACE_MIGRATOR_DB_PASSWORD is required}"
: "${FILE_DB_PASSWORD:?FILE_DB_PASSWORD is required}"
: "${FILE_MIGRATOR_DB_PASSWORD:?FILE_MIGRATOR_DB_PASSWORD is required}"
: "${EMAIL_DB_PASSWORD:?EMAIL_DB_PASSWORD is required}"
: "${EMAIL_MIGRATOR_DB_PASSWORD:?EMAIL_MIGRATOR_DB_PASSWORD is required}"

psql -v ON_ERROR_STOP=1 \
  --username "${POSTGRES_USER}" \
  --dbname "${POSTGRES_DB}" \
  --set=instance_app_password="${INSTANCE_DB_PASSWORD}" \
  --set=instance_migrator_password="${INSTANCE_MIGRATOR_DB_PASSWORD}" \
  --set=user_app_password="${USER_DB_PASSWORD}" \
  --set=user_migrator_password="${USER_MIGRATOR_DB_PASSWORD}" \
  --set=workspace_app_password="${WORKSPACE_DB_PASSWORD}" \
  --set=workspace_migrator_password="${WORKSPACE_MIGRATOR_DB_PASSWORD}" \
  --set=file_app_password="${FILE_DB_PASSWORD}" \
  --set=file_migrator_password="${FILE_MIGRATOR_DB_PASSWORD}" \
  --set=email_app_password="${EMAIL_DB_PASSWORD}" \
  --set=email_migrator_password="${EMAIL_MIGRATOR_DB_PASSWORD}" <<'SQL'

-- prevent public object creation in default db
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- ================
-- INSTANCE-SERVICE
-- ================
CREATE ROLE instance_migrator LOGIN PASSWORD :'instance_migrator_password';
CREATE ROLE instance_app LOGIN PASSWORD :'instance_app_password';

CREATE DATABASE syncturtle_instance OWNER instance_migrator;

\connect syncturtle_instance

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE syncturtle_instance FROM PUBLIC;

GRANT CONNECT ON DATABASE syncturtle_instance TO instance_app;
GRANT USAGE ON SCHEMA public TO instance_app;

ALTER DEFAULT PRIVILEGES FOR ROLE instance_migrator IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO instance_app;

ALTER DEFAULT PRIVILEGES FOR ROLE instance_migrator IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO instance_app;

-- ============
-- USER-SERVICE
-- ============

\connect postgres

CREATE ROLE user_migrator LOGIN PASSWORD :'user_migrator_password';
CREATE ROLE user_app LOGIN PASSWORD :'user_app_password';

CREATE DATABASE syncturtle_user OWNER user_migrator;

\connect syncturtle_user

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE syncturtle_user FROM PUBLIC;

GRANT CONNECT ON DATABASE syncturtle_user TO user_app;
GRANT USAGE ON SCHEMA public TO user_app;

ALTER DEFAULT PRIVILEGES FOR ROLE user_migrator IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO user_app;

ALTER DEFAULT PRIVILEGES FOR ROLE user_migrator IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO user_app;

-- =================
-- WORKSPACE-SERVICE
-- =================

\connect postgres

CREATE ROLE workspace_migrator LOGIN PASSWORD :'workspace_migrator_password';
CREATE ROLE workspace_app LOGIN PASSWORD :'workspace_app_password';

CREATE DATABASE syncturtle_workspace OWNER workspace_migrator;

\connect syncturtle_workspace

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE syncturtle_workspace FROM PUBLIC;

GRANT CONNECT ON DATABASE syncturtle_workspace TO workspace_app;
GRANT USAGE ON SCHEMA public TO workspace_app;

ALTER DEFAULT PRIVILEGES FOR ROLE workspace_migrator IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO workspace_app;

ALTER DEFAULT PRIVILEGES FOR ROLE workspace_migrator IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO workspace_app;

-- =============
-- EMAIL-SERVICE
-- =============

\connect postgres

CREATE ROLE email_migrator LOGIN PASSWORD :'email_migrator_password';
CREATE ROLE email_app LOGIN PASSWORD :'email_app_password';

CREATE DATABASE syncturtle_email OWNER email_migrator;

\connect syncturtle_email

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE syncturtle_email FROM PUBLIC;

GRANT CONNECT ON DATABASE syncturtle_email TO email_app;
GRANT USAGE ON SCHEMA public TO email_app;

ALTER DEFAULT PRIVILEGES FOR ROLE email_migrator IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO email_app;

ALTER DEFAULT PRIVILEGES FOR ROLE email_migrator IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO email_app;

-- ============
-- FILE-SERVICE
-- ============

\connect postgres

CREATE ROLE file_migrator LOGIN PASSWORD :'file_migrator_password';
CREATE ROLE file_app LOGIN PASSWORD :'file_app_password';

CREATE DATABASE syncturtle_file OWNER file_migrator;

\connect syncturtle_file

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE syncturtle_file FROM PUBLIC;

GRANT CONNECT ON DATABASE syncturtle_file TO file_app;
GRANT USAGE ON SCHEMA public TO file_app;

ALTER DEFAULT PRIVILEGES FOR ROLE file_migrator IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO file_app;

ALTER DEFAULT PRIVILEGES FOR ROLE file_migrator IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO file_app;

SQL