--liquibase formatted sql

--changeset syncturtle:0005-001-add-outbox-messages labels:instance
CREATE TABLE outbox_messages (
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    id                          UUID PRIMARY KEY,
    topic                       VARCHAR(255) NOT NULL,
    message_key                 VARCHAR(255) NOT NULL,
    event_type                  VARCHAR(120) NOT NULL,
    aggregate_type              VARCHAR(120) NOT NULL,
    aggregate_id                UUID NOT NULL,
    payload                     TEXT NOT NULL,
    status                      VARCHAR(30) NOT NULL,
    attempts                    INTEGER NOT NULL,
    max_attempts                INTEGER NOT NULL,
    next_attempt_at             TIMESTAMPTZ NOT NULL,
    published_at                TIMESTAMPTZ,
    locked_at                   TIMESTAMPTZ,
    locked_by                   VARCHAR(120),
    last_error                  TEXT,
    row_version                 BIGINT NOT NULL
);
--rollback DROP TABLE IF EXISTS outbox_messages;

--changeset syncturtle:0005-020-index-outbox-messages-due labels:instance
CREATE INDEX idx_outbox_messages_due
    ON outbox_messages (status, next_attempt_at, created_at);
--rollback DROP INDEX IF EXISTS idx_outbox_messages_due;

--changeset syncturtle:0005-021-index-outbox-messages-aggregate labels:instance
CREATE INDEX idx_outbox_messages_aggregate
    ON outbox_messages (aggregate_type, aggregate_id);
--rollback DROP INDEX IF EXISTS idx_outbox_messages_aggregate;

--changeset syncturtle:0005-022-index-outbox-messages-locked labels:instance
CREATE INDEX idx_outbox_messages_locked
    ON outbox_messages (status, locked_at);
--rollback DROP INDEX IF EXISTS idx_outbox_messages_locked;
