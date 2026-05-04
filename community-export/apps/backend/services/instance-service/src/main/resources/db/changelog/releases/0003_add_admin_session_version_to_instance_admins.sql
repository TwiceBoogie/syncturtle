--liquibase formatted sql

--changeset syncturtle:0003-001-add-session-version-to-instance-admins labels:instance
ALTER TABLE instance_admins ADD COLUMN session_version BIGINT NOT NULL DEFAULT 1;
--rollback ALTER TABLE instance_admins DROP COLUMN IF EXISTS session_version: