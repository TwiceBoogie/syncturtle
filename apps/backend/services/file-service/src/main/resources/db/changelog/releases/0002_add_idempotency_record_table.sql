--liquibase formatted sql

--changeset syncturtle:0002-001-add-idempotency-record-table labels:file
CREATE TABLE idempotency_record (
    created_at              TIMESTAMPTZ NOT NULL,
    id                      UUID PRIMARY KEY,
    idempotency_key         VARCHAR(128) NOT NULL,
    owner_user_id           UUID NOT NULL,
    route_key               VARCHAR(128) NOT NULL,
    request_hash            VARCHAR(64) NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    http_status             INTEGER,
    response_body           JSONB DEFAULT '{}'::jsonb,
    completed_at            TIMESTAMPTZ,
    expires_at              TIMESTAMPTZ NOT NULL,
    version                 BIGINT NOT NULL
);
--rollback DROP TABLE IF EXISTS idempotency_record;

--changeset syncturtle:0002-002-unique-idempotency-record-owner-route-key labels:file
ALTER TABLE idempotency_record
    ADD CONSTRAINT uq_idempotency_record_owner_route_key
    UNIQUE (owner_user_id, route_key, idempotency_key);
--rollback ALTER TABLE idempotency_record DROP CONSTRAINT IF EXISTS uq_idempotency_record_owner_route_key;

--changeset syncturtle:0002-020-index-idempotency-record-expires-at labels:file
CREATE INDEX idx_idempotency_record_expires_at
    ON idempotency_record(expires_at);
--rollback DROP INDEX IF EXISTS idx_idempotency_record_expires_at;

--changeset syncturtle:0002-021-index-idempotency-record-owner-route-created-at labels:file
CREATE INDEX idx_idempotency_record_owner_route_created_at
    ON idempotency_record(owner_user_id, route_key, created_at DESC);
--rollback DROP INDEX IF EXISTS idx_idempotency_record_owner_route_created_at;

--changeset syncturtle:0002-022-index-idempotency-record-status-created-at labels:file
CREATE INDEX idx_idempotency_record_status_created_at
    ON idempotency_record(status, created_at);
--rollback DROP INDEX IF EXISTS idx_idempotency_record_status_created_at;