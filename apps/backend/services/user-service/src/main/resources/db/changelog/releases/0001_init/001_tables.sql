--liquibase formatted sql

--changeset syncturtle:0001-001-create-table-login-policies labels:user
CREATE TABLE login_policies (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    policy_name                 VARCHAR(50) NOT NULL,
    max_attempts                INT NOT NULL,
    lockout_duration            INTERVAL,
    reset_duration              INTERVAL,
    is_default                  BOOLEAN NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    deleted_at                  TIMESTAMPTZ
);
--rollback DROP TABLE IF EXISTS login_policies;

--changeset syncturtle:0001-002-create-table-users labels:user
CREATE TABLE users (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    username                    VARCHAR(128) NOT NULL,
    mobile_number               VARCHAR(255),
    email                       VARCHAR(255),
    display_name                VARCHAR(255) NOT NULL,
    first_name                  VARCHAR(255),
    last_name                   VARCHAR(255),
    password                    VARCHAR(128) NOT NULL,
    avatar                      TEXT NOT NULL,
    cover_image                 VARCHAR(800),
    avatar_asset_id             UUID -- logical fk to file-service:file_assets.id,
    cover_image_asset_id        UUID -- logical fk to file-service:file_assets.id,
    last_location               VARCHAR(255) NOT NULL,
    created_location            VARCHAR(255) NOT NULL,
    is_managed                  BOOLEAN NOT NULL,
    is_password_expired         BOOLEAN NOT NULL,
    is_active                   BOOLEAN NOT NULL,
    is_email_verified           BOOLEAN NOT NULL,
    is_password_autoset         BOOLEAN NOT NULL,
    token                       VARCHAR(64) NOT NULL,
    user_timezone               VARCHAR(255) NOT NULL,
    last_active                 TIMESTAMPTZ,
    last_login_time             TIMESTAMPTZ,
    last_logout_time            TIMESTAMPTZ,
    last_login_ip               VARCHAR(255) NOT NULL,
    last_logout_ip              VARCHAR(255) NOT NULL,
    last_login_medium           VARCHAR(20) NOT NULL,
    last_login_uagent           TEXT NOT NULL,
    token_updated_at            TIMESTAMPTZ,
    is_bot                      BOOLEAN NOT NULL,
    bot_type                    VARCHAR(30),
    is_email_valid              BOOLEAN NOT NULL,
    masked_at                   TIMESTAMPTZ
);
--rollback DROP TABLE IF EXISTS users;

--changeset syncturtle:0001-003-create-table-profiles labels:user
CREATE TABLE profiles (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    theme                       JSONB NOT NULL,
    is_tour_completed           BOOLEAN NOT NULL,
    onboarding_step             JSONB NOT NULL,
    use_case                    TEXT,
    role                        VARCHAR(300),
    is_onboarded                BOOLEAN NOT NULL,
    last_workspace_id           UUID,
    billing_address_country     VARCHAR(255) NOT NULL,
    billing_address             JSONB,
    has_billing_address         BOOLEAN NOT NULL,
    company_name                VARCHAR(255) NOT NULL,
    user_id                     UUID NOT NULL,
    language                    VARCHAR(255) NOT NULL,
    has_marketing_email_consent BOOLEAN NOT NULL
);
--rollback DROP TABLE IF EXISTS profiles;

--changeset syncturtle:0001-004-create-table-accounts labels:user
CREATE TABLE accounts (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    provider_account_id         VARCHAR(255) NOT NULL,
    provider                    VARCHAR(50) NOT NULL,
    access_token                TEXT NOT NULL,
    access_token_expired_at     TIMESTAMPTZ,
    refresh_token               TEXT,
    refresh_token_expired_at    TIMESTAMPTZ,
    last_connected_at           TIMESTAMPTZ NOT NULL,
    metadata                    JSONB NOT NULL,
    user_id                     UUID NOT NULL,
    id_token                    TEXT NOT NULL
);
--rollback DROP TABLE IF EXISTS accounts;