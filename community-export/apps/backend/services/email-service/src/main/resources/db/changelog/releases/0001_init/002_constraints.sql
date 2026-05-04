--liquibase formatted sql

--changeset syncturtle:0001-010-unique-email-event-inbox-event-id labels:email
ALTER TABLE email_event_inbox
    ADD CONSTRAINT uq_email_event_inbox_event_id UNIQUE (event_id);
--rollback ALTER TABLE email_event_inbox DROP CONSTRAINT IF EXISTS uq_email_event_inbox_event_id;

--changeset syncturtle:0001-020-check-email-event-inbox-status labels:email
ALTER TABLE email_event_inbox
    ADD CONSTRAINT ck_email_event_inbox_status
    CHECK (status IN ('PROCESSING', 'SENT', 'FAILED_RETRYABLE', 'FAILED_PERMANENT'));
--rollback ALTER TABLE email_event_inbox DROP CONSTRAINT IF EXISTS ck_email_event_inbox_status;

--changeset syncturtle:0001-021-check-email-event-inbox-attempt-count labels:email
ALTER TABLE email_event_inbox
    ADD CONSTRAINT ck_email_event_inbox_attempt_count_non_negative
    CHECK (attempt_count >= 0);
--rollback ALTER TABLE email_event_inbox DROP CONSTRAINT IF EXISTS ck_email_event_inbox_attempt_count_non_negative;

--changeset syncturtle:0001-022-check-email-event-inbox-row-version labels:email
ALTER TABLE email_event_inbox
    ADD CONSTRAINT ck_email_event_inbox_row_version_non_negative
    CHECK (row_version >= 0);
--rollback ALTER TABLE email_event_inbox DROP CONSTRAINT IF EXISTS ck_email_event_inbox_row_version_non_negative;

--changeset syncturtle:0001-023-check-email-event-inbox-processing-has-lock-until labels:email
ALTER TABLE email_event_inbox
    ADD CONSTRAINT ck_email_event_inbox_processing_has_lock_until
    CHECK (status <> 'PROCESSING' OR lock_until IS NOT NULL);
--rollback ALTER TABLE email_event_inbox DROP CONSTRAINT IF EXISTS ck_email_event_inbox_processing_has_lock_until;

--changeset syncturtle:0001-024-check-email-event-inbox-retryable-has-next-attempt-at labels:email
ALTER TABLE email_event_inbox
    ADD CONSTRAINT ck_email_event_inbox_retryable_has_next_attempt_at
    CHECK (status <> 'FAILED_RETRYABLE' OR next_attempt_at IS NOT NULL);
--rollback ALTER TABLE email_event_inbox DROP CONSTRAINT IF EXISTS ck_email_event_inbox_retryable_has_next_attempt_at;

--changeset syncturtle:0001-025-check-email-event-inbox-terminal-has-processed-at labels:email
ALTER TABLE email_event_inbox
    ADD CONSTRAINT ck_email_event_inbox_terminal_has_processed_at
    CHECK (status NOT IN ('SENT', 'FAILED_PERMANENT') OR processed_at IS NOT NULL);
--rollback ALTER TABLE email_event_inbox DROP CONSTRAINT IF EXISTS ck_email_event_inbox_terminal_has_processed_at;