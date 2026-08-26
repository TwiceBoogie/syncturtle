--liquibase formatted sql

--changeset syncturtle:0004-001-add-file-asset-storage-version labels:file
ALTER TABLE file_assets
    ADD COLUMN storage_version_id VARCHAR(255);

UPDATE file_assets
SET storage_version_id = 'null'
WHERE status = 'UPLOADED'
  AND storage_version_id IS NULL;

ALTER TABLE file_assets
    ADD CONSTRAINT ck_file_assets_uploaded_storage_version
    CHECK (status <> 'UPLOADED' OR storage_version_id IS NOT NULL);
--rollback ALTER TABLE file_assets DROP CONSTRAINT IF EXISTS ck_file_assets_uploaded_storage_version;
--rollback ALTER TABLE file_assets DROP COLUMN IF EXISTS storage_version_id;

--changeset syncturtle:0004-020-index-file-asset-unlinked-uploaded-cleanup labels:file
CREATE INDEX idx_file_assets_unlinked_uploaded_cleanup
    ON file_assets(uploaded_at, upload_expires_at)
    WHERE status = 'UPLOADED'
      AND is_deleted = false
      AND storage_deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS idx_file_assets_unlinked_uploaded_cleanup;
