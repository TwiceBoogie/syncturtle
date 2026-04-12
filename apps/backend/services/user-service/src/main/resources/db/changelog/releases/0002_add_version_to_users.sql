--liquibase formatted sql

--changeset syncturtle:0002-001-add-version-column labels:user
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL;
--rollback ALTER TABLE users DROP COLUMN IF EXISTS version;