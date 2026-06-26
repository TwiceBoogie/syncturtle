--liquibase formatted sql

--changeset syncturtle:0004-001-add-workspace-lite labels:instance
CREATE TABLE workspaces_lite (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    source_version              BIGINT NOT NULL,
    name                        VARCHAR(80) NOT NULL,
    logo_asset_id               UUID, -- logical fk to file-service:file_assets.id
    slug                        VARCHAR(48) NOT NULL,
    organization_size           VARCHAR(20),
    owner_id                    UUID NOT NULL, -- logical fk to user-service:users.id
    timezone                    VARCHAR(255) NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    deleted_at                  TIMESTAMPTZ,
    projected_at                TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE IF EXISTS workspace_lite;

--changeset syncturtle:0004-002-create-table-workspace-members-lite labels:instance
CREATE TABLE workspace_members_lite (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    workspace_id                UUID NOT NULL,
    member_id                   UUID NOT NULL,
    role                        INTEGER NOT NULL,
    is_active                   BOOLEAN NOT NULL,
    source_version              BIGINT NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    deleted_at                  TIMESTAMPTZ,
    projected_at                TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE IF EXISTS workspace_members_lite;

--changeset syncturtle:0004-010-index-workspace-members-lite-active labels:instance
CREATE INDEX idx_workspace_members_lite_member_active
    ON workspace_members_lite (member_id, is_active, deleted_at);
--rollback DROP INDEX IF EXISTS idx_workspace_members_lite_member_active;