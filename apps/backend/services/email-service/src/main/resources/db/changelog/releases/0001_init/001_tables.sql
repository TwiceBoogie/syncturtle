--liquibase formatted sql

--changeset syncturtle:0001-001-create-email-event-inbox labels:email
CREATE TABLE email_event_inbox (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    event_id                    VARCHAR(100) NOT NULL,
    event_type                  VARCHAR(100) NOT NULL,
    correlation_id              VARCHAR(100),
    template_type               VARCHAR(50) NOT NULL,
    subject                     VARCHAR(255) NOT NULL,
    recipient_to_json           TEXT NOT NULL,
    template_model_json         TEXT NOT NULL,
    status                      VARCHAR(32) NOT NULL,
    attempt_count               INTEGER NOT NULL DEFAULT 0,
    lock_until                  TIMESTAMPTZ,
    next_attempt_at             TIMESTAMPTZ,
    processed_at                TIMESTAMPTZ,
    last_error                  TEXT,
    row_version                 BIGINT NOT NULL DEFAULT 0
);
--rollback DROP TABLE IF EXISTS email_event_inbox;