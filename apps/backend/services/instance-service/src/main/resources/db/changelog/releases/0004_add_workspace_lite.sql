--liquibase formatted sql

--changeset syncturtle:0004-001-add-workspace-lite labels:instance
CREATE TABLE workspaces_lite (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    version                     BIGINT NOT NULL,
    name                        VARCHAR(80) NOT NULL,
    logo_asset_id                UUID, -- logical fk to file-service:file_assets.id
    slug                        VARCHAR(48) NOT NULL,
    organization_size           VARCHAR(20),
    owner_id                    UUID NOT NULL, -- logical fk to user-service:users.id
    timezone                    VARCHAR(255) NOT NULL,
    total_members               BIGINT NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    deleted_at                  TIMESTAMPTZ,
    projected_at                TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE IF EXISTS workspace_lite;