--liquibase formatted sql

--changeset syncturtle:0001-010-unique-file-assets-storage-object labels:file
ALTER TABLE file_assets
    ADD CONSTRAINT uq_file_assets_storage_object
    UNIQUE (storage_provider, bucket, object_key);
--rollback ALTER TABLE file_assets DROP CONSTRAINT IF EXISTS uq_file_assets_storage_object;

--changeset syncturtle:0001-011-fk-file-asset-links-file-assets labels:file
ALTER TABLE file_asset_links
    ADD CONSTRAINT fk_file_asset_links_file_assets
    FOREIGN KEY (asset_id) REFERENCES file_assets(id)
    ON DELETE CASCADE
    DEFERRABLE INITIALLY DEFERRED;
--rollback ALTER TABLE file_asset_links DROP CONSTRAINT IF EXISTS fk_file_asset_links_file_assets;