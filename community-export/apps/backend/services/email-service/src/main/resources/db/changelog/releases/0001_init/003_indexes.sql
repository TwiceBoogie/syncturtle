--liquibase formatted sql

--changeset syncturtle:0001-040-index-email-event-inbox-due-retry labels:email
CREATE INDEX idx_email_event_inbox_due_retry
    ON email_event_inbox(next_attempt_at, id)
    WHERE status = 'FAILED_RETRYABLE';
--rollback DROP INDEX IF EXISTS idx_email_event_inbox_due_retry;

--changeset syncturtle:0001-041-index-email-event-inbox-expired-processing-lease labels:email
CREATE INDEX idx_email_event_inbox_expired_processing_lease
    ON email_event_inbox(lock_until, id)
    WHERE status = 'PROCESSING';
--rollback DROP INDEX IF EXISTS idx_email_event_inbox_expired_processing_lease;

--changeset syncturtle:0001-042-index-email-event-inbox-terminal-processing-at labels:email
CREATE INDEX idx_email_event_inbox_terminal_processed_at
    ON email_event_inbox(processed_at, id)
    WHERE status IN ('SENT', 'FAILED_PERMANENT');
--rollback DROP INDEX IF EXISTS idx_email_event_inbox_terminal_processed_at;

--changeset syncturtle:0001-043-index-email-event-inbox-correlation-id labels:email
CREATE INDEX idx_email_event_inbox_correlation_id
    ON email_event_inbox(correlation_id);
--rollback DROP INDEX IF EXISTS idx_email_event_inbox_correlation_id;