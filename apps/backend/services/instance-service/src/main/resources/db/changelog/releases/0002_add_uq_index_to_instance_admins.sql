--liquibase formatted sql

--changeset syncturtle:0002-001-unique-owner-per-instance labels:instance
CREATE UNIQUE INDEX uq_instance_admins_owner_per_instance
    ON instance_admins (instance_id)
    WHERE role = 20 AND deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS uq_instance_admins_owner_per_instance;