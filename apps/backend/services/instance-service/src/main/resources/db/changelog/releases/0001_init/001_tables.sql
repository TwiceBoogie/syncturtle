--liquibase formatted sql

--changeset syncturtle:0001-001-create-instances labels:instance
CREATE TABLE instances (
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    -- e.g. "Syncturtle Enterprise" instance_name
    instance_name           VARCHAR(100),
    -- e.g. "syncturtle-enterprise-01"; unique among active rows
    slug                    VARCHAR(100) UNIQUE,
    -- community|cloud|enterprise
    edition                 VARCHAR(50) NOT NULL,
    -- app binary (what is deployed)
    binary_release_tag      VARCHAR(50) NOT NULL DEFAULT 'UNRELEASED', -- e.g. "v0.3.0"
    binary_version          VARCHAR(50) NOT NULL, -- e.g. "0.3.0"
    binary_commit           VARCHAR(40), -- full SHA
    binary_commit_short     VARCHAR(12),
    binary_built_at         TIMESTAMPTZ,
    -- optional build metadata
    binary_branch           TEXT,
    binary_dirty            BOOLEAN,
    binary_build_id         TEXT, -- CI run id/build number
    binary_ci               TEXT, -- "github-actions"
    binary_platform         TEXT, -- "linux-amd64"
    -- update checking
    latest_release_tag      VARCHAR(50),
    latest_version          VARCHAR(50),
    last_checked_at         TIMESTAMPTZ,
    -- config version
    config_version          BIGINT NOT NULL,
    config_last_checked_at  TIMESTAMPTZ,
    version                 BIGINT NOT NULL,
    domain                  TEXT,
    namespace               VARCHAR(255),
    -- unique and each enterprise deployment would have one
    instance_id             VARCHAR(255) NOT NULL,
    -- e.g. vm-42-internal
    vm_host                 TEXT,
    -- master consent; if false no otlp traces, posthog/analytics + force IS_INTERCOM_ENABLED='0'
    is_telemetry_enabled    BOOLEAN NOT NULL,
    -- this instance expects vendor support
    is_support_required     BOOLEAN NOT NULL,
    is_setup_done           BOOLEAN NOT NULL,
    is_verified             BOOLEAN NOT NULL,
    is_test                 BOOLEAN NOT NULL,
    deleted_at              TIMESTAMPTZ
);
--rollback DROP TABLE IF EXISTS instances;

--changeset syncturtle:0001-002-create-instance-configurations labels:instance
CREATE TABLE instance_configurations (
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    key                     TEXT NOT NULL,
    value                   TEXT,
    category                TEXT NOT NULL,
    is_encrypted            BOOLEAN NOT NULL
);
--rollback DROP TABLE IF EXISTS instance_configurations;

--changeset syncturtle:0001-003-create-users-lite labels:instance
CREATE TABLE users_lite (
    id                      UUID PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL,
    first_name              VARCHAR(36) NOT NULL,
    last_name               VARCHAR(36) NOT NULL,
    display_name            VARCHAR(255) NOT NULL,
    date_joined             TIMESTAMPTZ NOT NULL,
    version                 BIGINT NOT NULL
);
--rollback DROP TABLE IF EXISTS users_lite;

--changeset syncturtle:0001-004-create-instance-admins labels:instance
CREATE TABLE instance_admins (
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    instance_id             UUID NOT NULL, -- fk at app-level
    user_id                 UUID NOT NULL, -- logical fk to user-service.users.id
    role                    INTEGER NOT NULL,
    is_verified             BOOLEAN NOT NULL,
    deleted_at              TIMESTAMPTZ
);
--rollback DROP TABLE IF EXISTS instance_admins;