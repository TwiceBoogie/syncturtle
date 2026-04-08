--liquibase formatted sql

--changeset syncturtle:0003-001-create-instance-configuration-scope-versions labels:instance
CREATE TABLE instance_configuration_scope_versions (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    scope                       VARCHAR(100) NOT NULL UNIQUE,
    version                     BIGINT NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    deleted_at                  TIMESTAMPTZ
);
--rollback DROP TABLE IF EXISTS instance_configuration_scope_versions;
