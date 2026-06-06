--liquibase formatted sql

--changeset syncturtle:0003-001-create-table-instances-lite labels:user
CREATE TABLE instances_lite (
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    id              UUID PRIMARY KEY,
    edition         VARCHAR(50) NOT NULL,
    is_setup_done   BOOLEAN NOT NULL,
    version         BIGINT NOT NULL
);
--rollback DROP TABLE IF EXISTS instances_lite;