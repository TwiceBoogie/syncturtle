--liquibase formatted sql

--changeset syncturtle:0001-020-index-unique-file-asset-links-primary-usage labels:file
CREATE UNIQUE INDEX uq_file_asset_links_primary_usage
    ON file_asset_links(target_service, target_type, target_id, usage_type)
    WHERE is_primary = true;
--rollback DROP INDEX IF EXISTS uq_file_asset_links_primary_usage;