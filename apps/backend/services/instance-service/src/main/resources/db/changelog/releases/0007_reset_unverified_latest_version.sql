--liquibase formatted sql

--changeset syncturtle:0007-001-reset-unverified-latest labels:instance
ALTER TABLE instances ALTER COLUMN last_checked_at DROP NOT NULL;

-- Prior setup copied current_version to latest_version without checking a release.
UPDATE instances
SET latest_version = NULL,
    last_checked_at = NULL
WHERE latest_version = current_version
  AND last_checked_at IS NOT NULL;