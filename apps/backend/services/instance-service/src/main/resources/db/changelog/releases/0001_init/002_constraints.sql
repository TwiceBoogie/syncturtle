--liquibase formatted sql

--changeset syncturtle:0001-010-unique-instances-instance-id labels:instance
ALTER TABLE instances
    ADD CONSTRAINT uq_instances_instance_id UNIQUE (instance_id);
--rollback ALTER TABLE instances DROP CONSTRAINT IF EXISTS uq_instances_instance_id;

--changeset syncturtle:0001-011-unique-instance-admins-instance-id-user-id labels:instance
ALTER TABLE instance_admins
    ADD CONSTRAINT uq_instance_admins_instance_id_user_id UNIQUE (instance_id, user_id);
--rollback ALTER TABLE instance_admins DROP CONSTRAINT IF EXISTS uq_instance_admins_instance_id_user_id;

--changeset syncturtle:0001-012-unique-instance-config-key labels:instance
ALTER TABLE instance_configurations
    ADD CONSTRAINT uq_instance_configurations_key UNIQUE (key);
--rollback ALTER TABLE instance_configurations DROP CONSTRAINT IF EXISTS uq_instance_configurations_key;

--changeset syncturtle:0001-020-check-instance-admins-role-non-negative labels:instance
ALTER TABLE instance_admins
    ADD CONSTRAINT ck_instance_admins_role_non_negative
    CHECK (role >= 0);
--rollback ALTER TABLE instance_admins DROP CONSTRAINT IF EXISTS ck_instance_admins_role_non_negative

--changeset syncturtle:0001-030-fk-instance-admins-instance labels:instance
ALTER TABLE instance_admins
    ADD CONSTRAINT fk_instance_admins_instance
    FOREIGN KEY (instance_id) REFERENCES instances(id)
    DEFERRABLE INITIALLY DEFERRED;
--rollback ALTER TABLE instance_admins DROP CONSTRAINT IF EXISTS fk_instance_admins_instance;