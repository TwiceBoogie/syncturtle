--liquibase formatted sql

--changeset syncturtle:0001-020-index-file-assets-workspace-created-at labels:file
CREATE INDEX idx_file_assets_workspace_created_at
    ON file_assets(workspace_id, created_at DESC)
    WHERE is_deleted = false;
--rollback DROP INDEX IF EXISTS idx_file_assets_workspace_created_at;

--changeset syncturtle:0001-021-index-file-assets-owner-created-at labels:file
CREATE INDEX idx_file_assets_owner_created_at
    ON file_assets(owner_user_id, created_at DESC)
    WHERE is_deleted = false;
--rollback DROP INDEX IF EXISTS idx_file_assets_owner_created_at;

--changeset syncturtle:0001-022-index-file-assets-status-upload-expires-at labels:file
CREATE INDEX idx_file_assets_status_upload_expires_at
    ON file_assets(status, upload_expires_at)
    WHERE is_deleted = false;
--rollback DROP INDEX IF EXISTS idx_file_assets_status_upload_expires_at;

--changeset syncturtle:0001-023-index-file-assets-purpose-created-at labels:file
CREATE INDEX idx_file_assets_purpose_created_at
    ON file_assets(purpose, created_at DESC)
    WHERE is_deleted = false;
--rollback DROP INDEX IF EXISTS idx_file_assets_purpose_created_at;

--changeset syncturtle:0001-024-index-file-assets-checksum-sha256 labels:file
CREATE INDEX idx_file_assets_checksum_sha256
    ON file_assets(checksum_sha256)
    WHERE checksum_sha256 IS NOT NULL
      AND is_deleted = false;
--rollback DROP INDEX IF EXISTS idx_file_assets_checksum_sha256;

--changeset syncturtle:0001-025-index-file-asset-links-asset-id labels:file
CREATE INDEX idx_file_asset_links_asset_id
    ON file_asset_links(asset_id)
    WHERE asset_id IS NOT NULL;
--rollback DROP INDEX IF EXISTS idx_file_asset_links_asset_id;

--changeset syncturtle:0001-026-index-file-asset-links-active-target labels:file
CREATE INDEX idx_file_asset_links_active_target
    ON file_asset_links(target_service, target_type, target_id)
    WHERE deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS idx_file_asset_links_active_target;

--changeset syncturtle:0001-027-index-file-asset-links-workspace-created-at labels:file
CREATE INDEX idx_file_asset_links_workspace_created_at
    ON file_asset_links(workspace_id, created_at DESC)
    WHERE workspace_id IS NOT NULL
      AND deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS idx_file_asset_links_workspace_created_at;

--changeset syncturtle:0001-028-index-unique-file-asset-links-active-target-asset-usage labels:file
CREATE UNIQUE INDEX uq_file_asset_links_active_target_asset_usage
    ON file_asset_links(target_service, target_type, target_id, asset_id, usage_type)
    WHERE asset_id IS NOT NULL
      AND deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS uq_file_asset_links_active_target_asset_usage;

--changeset syncturtle:0001-029-index-unique-file-asset-links-primary-slot labels:file
CREATE UNIQUE INDEX uq_file_asset_links_primary_slot
    ON file_asset_links(target_service, target_type, target_id, usage_type)
    WHERE is_primary = true;
--rollback DROP INDEX IF EXISTS uq_file_asset_links_primary_slot;