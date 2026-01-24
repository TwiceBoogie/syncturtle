--liquibase formatted sql

--changeset syncturtle:0002-001-add-field-version-table-users labels:user
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL;
--rollback ALTER TABLE users DROP COLUMN version;