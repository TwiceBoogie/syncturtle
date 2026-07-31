--liquibase formatted sql

--changeset syncturtle:0008-001-add-outbox-trace-context labels:user
ALTER TABLE outbox_messages
    ADD COLUMN traceparent VARCHAR(55),
    ADD COLUMN tracestate VARCHAR(512);
--rollback ALTER TABLE outbox_messages DROP COLUMN IF EXISTS tracestate, DROP COLUMN IF EXISTS traceparent;
