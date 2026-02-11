--liquibase formatted sql

--changeset syncturtle:0001-001-create-table-workspaces labels:workspace
CREATE TABLE workspaces (
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    id                  UUID PRIMARY KEY,
    name                VARCHAR(80) NOT NULL,
    logo                TEXT,
    logo_asset_id       UUID, -- logical fk to file-service:file_assets.id
    slug                VARCHAR(48) NOT NULL,
    organization_size   VARCHAR(20),
    owner_id            UUID NOT NULL, -- logical fk to user-service:users.id
    timezone            VARCHAR(255) NOT NULL,
    created_by_id       UUID,
    updated_by_id       UUID,
    deleted_at          TIMESTAMPTZ,
    version             BIGINT NOT NULL
);
--rollback DROP TABLE IF EXISTS workspaces;

--changeset syncturtle:0001-002-create-table-workspace-member-invites labels:workspace
CREATE TABLE workspace_member_invites (
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL,
    accepted                BOOLEAN NOT NULL,
    token                   VARCHAR(255) NOT NULL,
    message                 TEXT,
    responded_at            TIMESTAMPTZ,
    role                    SMALLINT NOT NULL,
    workspace_id            UUID NOT NULL,
    created_by_id           UUID,
    updated_by_id           UUID,
    deleted_at              TIMESTAMPTZ
);
--rollback DROP TABLE IF EXISTS workspace_member_invites;

--changeset syncturtle:0001-003-create-table-workspace-members labels:workspace
CREATE TABLE workspace_members (
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    role                    SMALLINT NOT NULL,
    member_id               UUID NOT NULL, -- logical fk to user-service:users.id
    workspace_id            UUID NOT NULL,
    company_role            TEXT,
    is_active               BOOLEAN NOT NULL,
    created_by_id           UUID,
    updated_by_id           UUID,
    deleted_at              TIMESTAMPTZ,
    CONSTRAINT workspace_member_role_check CHECK ((role >= 0))
);
--rollback DROP TABLE IF EXISTS workspace_members;

--changeset syncturtle:0001-004-create-table-users-lite labels:workspace
CREATE TABLE users_lite (
    id                      UUID PRIMARY KEY,
    username                VARCHAR(128) NOT NULL,
    email                   VARCHAR(255),
    display_name            VARCHAR(255) NOT NULL,
    first_name              VARCHAR(36),
    last_name               VARCHAR(36),
    date_joined             TIMESTAMPTZ NOT NULL,
    avatar_asset_id         UUID, -- logical fk to file-service:file_assets.id,
    cover_image_asset_id    UUID, -- logical fk to file-service:file_assets.id,
    is_active               BOOLEAN NOT NULL,
    is_email_verified       BOOLEAN NOT NULL,
    is_password_autoset     BOOLEAN NOT NULL,
    user_timezone           VARCHAR(255) NOT NULL,
    is_bot                  BOOLEAN NOT NULL,
    version                 BIGINT NOT NULL
);
--rollback DROP TABLE IF EXISTS users_lite;

--changeset syncturtle:0001-005-create-table-instances-lite labels:workspace
CREATE TABLE instances_lite (
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    edition                 VARCHAR(50) NOT NULL,
    is_setup_done           BOOLEAN NOT NULL,
    version                 BIGINT NOT NULL
);
--rollback DROP TABLE IF EXISTS instances_lite;