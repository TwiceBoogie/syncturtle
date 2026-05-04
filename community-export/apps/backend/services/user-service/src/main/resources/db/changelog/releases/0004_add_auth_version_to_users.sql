--liquibase formatted sql

--changeset syncturtle:0004-001-add-auth-version-to-users labels:user
ALTER TABLE users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 1;
--rollback ALTER TABLE users DROP COLUMN IF EXISTS auth_version;