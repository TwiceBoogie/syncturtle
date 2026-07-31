--liquibase formatted sql

--changeset syncturtle:0007-001-create-table-password-reset-tokens labels:users,authentication
CREATE TABLE password_reset_tokens (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    user_id                     UUID NOT NULL,
    token_hash                  VARCHAR(64) NOT NULL,
    issued_for_auth_version     BIGINT NOT NULL,
    expires_at                  TIMESTAMPTZ NOT NULL,
    consumed_at                 TIMESTAMPTZ,
    invalidated_at              TIMESTAMPTZ,
    version                     BIGINT NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    deleted_at                  TIMESTAMPTZ
);
--rollback DROP TABLE IF EXISTS password_reset_tokens;