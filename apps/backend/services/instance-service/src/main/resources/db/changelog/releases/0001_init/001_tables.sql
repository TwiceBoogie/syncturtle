--liquibase formatted sql

--changeset syncturtle:0001-001-create-instances labels:instance
CREATE TABLE instances (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    instance_name               VARCHAR(255) NOT NULL,
    whitelist_emails            TEXT,
    instance_id                 VARCHAR(255) NOT NULL,
    current_version             VARCHAR(255) NOT NULL,
    last_checked_at             TIMESTAMPTZ NOT NULL,
    namespace                   VARCHAR(255),
    -- master consent; if false no otlp traces, posthog/analytics + force IS_INTERCOM_ENABLED='0'
    is_telemetry_enabled        BOOLEAN NOT NULL,
    is_support_required         BOOLEAN NOT NULL,
    is_setup_done               BOOLEAN NOT NULL,
    is_signup_screen_visited    BOOLEAN NOT NULL,
    is_verified                 BOOLEAN NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    domain                      TEXT NOT NULL,
    latest_version              VARCHAR(50),
    edition                     VARCHAR(50) NOT NULL,
    config_version              BIGINT NOT NULL,
    config_last_checked_at      TIMESTAMPTZ,
    version                     BIGINT NOT NULL,
    vm_host                     TEXT,
    deleted_at                  TIMESTAMPTZ,
    is_test                     BOOLEAN NOT NULL
);
--rollback DROP TABLE IF EXISTS instances;

--changeset syncturtle:0001-002-create-instance-configurations labels:instance
CREATE TABLE instance_configurations (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    key                         VARCHAR(100) NOT NULL,
    value                       TEXT,
    category                    TEXT NOT NULL,
    is_encrypted                BOOLEAN NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    deleted_at                  TIMESTAMPTZ
);
--rollback DROP TABLE IF EXISTS instance_configurations;

--changeset syncturtle:0001-003-create-users-lite labels:instance
CREATE TABLE users_lite (
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    username                VARCHAR(128) NOT NULL,
    email                   VARCHAR(255),
    display_name            VARCHAR(255) NOT NULL,
    first_name              VARCHAR(36),
    last_name               VARCHAR(36),
    user_timezone           VARCHAR(255) NOT NULL,
    principal_type          VARCHAR(40) NOT NULL,
    avatar_asset_id         UUID, -- logical fk to file-service:file_assets.id,
    cover_image_asset_id    UUID, -- logical fk to file-service:file_assets.id,
    is_active               BOOLEAN NOT NULL,
    is_email_verified       BOOLEAN NOT NULL,
    is_password_autoset     BOOLEAN NOT NULL,
    last_login_medium       VARCHAR(20),
    source_version          BIGINT NOT NULL,
    auth_version            BIGINT NOT NULL,
    deleted_at              TIMESTAMPTZ,
    projected_at            TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE IF EXISTS users_lite;

--changeset syncturtle:0001-004-create-instance-admins labels:instance
CREATE TABLE instance_admins (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    role                        INTEGER NOT NULL,
    is_verified                 BOOLEAN NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    instance_id                 UUID NOT NULL, -- fk at app-level
    user_id                     UUID, -- logical fk to user-service.users.id
    session_version             BIGINT NOT NULL,
    deleted_at                  TIMESTAMPTZ
);
--rollback DROP TABLE IF EXISTS instance_admins;