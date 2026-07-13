--liquibase formatted sql

--changeset syncturtle:0005-001-create-table-workspaces-lite labels:user
CREATE TABLE workspaces_lite (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    source_version              BIGINT NOT NULL,
    name                        VARCHAR(80) NOT NULL,
    logo_asset_id               UUID, -- logical fk to file-service:file_assets.id
    slug                        VARCHAR(48) NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    deleted_at                  TIMESTAMPTZ,
    projected_at                TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE IF EXISTS workspaces_lite;

--changeset syncturtle:0005-002-create-table-workspace-members-lite labels:user
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

--changeset syncturtle:0005-003-create-table-workspace-member_invites-lite labels:user
CREATE TABLE workspace_member_invites_lite (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    workspace_id                UUID NOT NULL,
    email                       VARCHAR(320) NOT NULL,
    accepted                    BOOLEAN NOT NULL,
    role                        INTEGER NOT NULL,
    responded_at                TIMESTAMPTZ,
    source_version              BIGINT NOT NULL,
    created_by_id               UUID,
    updated_by_id               UUID,
    deleted_at                  TIMESTAMPTZ,
    projected_at                TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE IF EXISTS workspace_member_invites_lite;

--changeset syncturtle:0005-010-index-workspaces-lite-deleted-at labels:users
CREATE INDEX idx_workspaces_lite_deleted_at
    ON workspaces_lite (deleted_at);
--rollback DROP INDEX IF EXISTS idx_workspaces_lite_deleted_at;

--changeset syncturtle:0005-011-index-workspace-members-lite-active labels:users
CREATE INDEX idx_workspace_members_lite_member_active
    ON workspace_members_lite (member_id, is_active, deleted_at);
--rollback DROP INDEX IF EXISTS idx_workspace_members_lite_member_active;

--changeset syncturtle:0005-012-index-workspace-members-lite-workspace-member labels:users
CREATE INDEX idx_workspace_members_lite_workspace_member
    ON workspace_members_lite (workspace_id, member_id);
--rollback DROP INDEX IF EXISTS idx_workspace_members_lite_workspace_member;

--changeset syncturtle:0005-013-index-workspace-member-invites-lite-open-email labels:users
CREATE INDEX idx_workspace_member_invites_lite_open_email
    ON workspace_member_invites_lite (email)
    WHERE accepted = false AND deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS idx_workspace_member_invites_lite_open_email;