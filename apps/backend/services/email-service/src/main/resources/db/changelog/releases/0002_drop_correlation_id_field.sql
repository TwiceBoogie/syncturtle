--liquibase formatted sql

--changeset syncturtle:0002-001-drop-correlationId labels:email
--comment: Dropping redundant correlationId column from email_event_inbox
ALTER TABLE email_event_inbox DROP COLUMN correlation_id;
--rollback ALTER TABLE email_event_inbox ADD correlation_id VARCHAR(255);