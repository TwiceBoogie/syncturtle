--liquibase formatted sql

--changeset syncturtle:0003-001-add-file-asset-storage-cleanup-state labels:file
ALTER TABLE file_assets
    ADD COLUMN storage_cleanup_token UUID,
    ADD COLUMN storage_cleanup_claimed_until TIMESTAMPTZ,
    ADD COLUMN storage_deleted_at TIMESTAMPTZ;
--rollback ALTER TABLE file_assets DROP COLUMN IF EXISTS storage_deleted_at, DROP COLUMN IF EXISTS storage_cleanup_claimed_until, DROP COLUMN IF EXISTS storage_cleanup_token;

--changeset syncturtle:0003-002-add-idempotency-processing-lease labels:file
ALTER TABLE idempotency_record
    ADD COLUMN processing_token UUID,
    ADD COLUMN processing_expires_at TIMESTAMPTZ;
--rollback ALTER TABLE idempotency_record DROP COLUMN IF EXISTS processing_expires_at, DROP COLUMN IF EXISTS processing_token;

--changeset syncturtle:0003-020-index-file-asset-storage-cleanup-candidates labels:file
CREATE INDEX idx_file_assets_storage_cleanup_candidates
    ON file_assets(upload_expires_at, storage_cleanup_claimed_until)
    WHERE storage_deleted_at IS NULL
      AND (status = 'PENDING_UPLOAD' OR is_deleted = true);
--rollback DROP INDEX IF EXISTS idx_file_assets_storage_cleanup_candidates;

--changeset syncturtle:0003-021-index-idempotency-processing-expiration labels:file
CREATE INDEX idx_idempotency_record_processing_expiration
    ON idempotency_record(processing_expires_at)
    WHERE status = 'PROCESSING';
--rollback DROP INDEX IF EXISTS idx_idempotency_record_processing_expiration;
