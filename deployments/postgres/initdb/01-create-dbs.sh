#!/usr/bin/env bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username postgres <<'SQL'
CREATE DATABASE syncturtle_instance;
CREATE USER instance_svc WITH ENCRYPTED PASSWORD 'instance_svc_dev';
GRANT ALL PRIVILEGES ON DATABASE syncturtle_instance TO instance_svc;

\connect syncturtle_instance
CREATE SCHEMA IF NOT EXISTS instance AUTHORIZATION instance_svc;
CREATE SCHEMA IF NOT EXISTS instance_liquibase AUTHORIZATION instance_svc;
GRANT USAGE, CREATE ON SCHEMA instance TO instance_svc;
GRANT USAGE, CREATE ON SCHEMA instance_liquibase TO instance_svc;

CREATE DATABASE syncturtle_user;
CREATE USER user_svc WITH ENCRYPTED PASSWORD 'user_svc_dev';
GRANT ALL PRIVILEGES ON DATABASE syncturtle_user TO user_svc;
SQL

