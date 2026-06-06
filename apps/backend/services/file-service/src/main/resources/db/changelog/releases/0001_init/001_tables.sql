--liquibase formatted sql

--changeset syncturtle:0001-001-create-table-file-assets labels:file
CREATE TABLE file_assets (
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    instance_id             UUID,
    workspace_id            UUID,
    owner_user_id           UUID,
    storage_provider        VARCHAR(32) NOT NULL,
    bucket                  VARCHAR(255) NOT NULL,
    object_key              VARCHAR(1000) NOT NULL,
    original_filename       VARCHAR(255) NOT NULL,
    content_type            VARCHAR(127) NOT NULL,
    extension               VARCHAR(20),
    declared_size_bytes     BIGINT NOT NULL,
    actual_size_bytes       BIGINT,
    checksum_sha256         VARCHAR(64),
    purpose                 VARCHAR(64) NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    upload_expires_at       TIMESTAMPTZ,
    uploaded_at             TIMESTAMPTZ,
    is_deleted              BOOLEAN NOT NULL DEFAULT false,
    deleted_at              TIMESTAMPTZ,
    attributes              JSONB NOT NULL DEFAULT '{}'::jsonb,
    storage_metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by_id           UUID,
    updated_by_id           UUID,
    version                 BIGINT NOT NULL DEFAULT 1
);
--rollback DROP TABLE IF EXISTS file_assets;

--changeset syncturtle:0001-002-create-file-asset-links labels:file
CREATE TABLE file_asset_links (
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    asset_id                UUID NOT NULL REFERENCES file_assets(id) ON DELETE CASCADE,
    workspace_id            UUID,
    target_service          VARCHAR(64) NOT NULL,
    target_type             VARCHAR(64) NOT NULL,
    target_id               UUID NOT NULL,
    usage_type              VARCHAR(64) NOT NULL,
    is_primary              BOOLEAN NOT NULL DEFAULT false,
    linked_by_user_id       UUID,
    attributes              JSONB NOT NULL DEFAULT '{}'::jsonb
);
--rollback DROP TABLE IF EXISTS file_asset_links;