--liquibase formatted sql

--changeset syncturtle:0001-040-index-instance-admins-created-by-id labels:instance
CREATE INDEX idx_instance_admins_created_by_id
    ON instance_admins(created_by_id);
--rollback DROP INDEX IF EXISTS idx_instance_admins_created_by_id;

--changeset syncturtle:0001-041-index-instance-admins-instance-id labels:instance
CREATE INDEX idx_instance_admins_instance_id
    ON instance_admins(instance_id);
--rollback DROP INDEX IF EXISTS idx_instance_admins_instance_id;

--changeset syncturtle:0001-042-index-instance-admins-updated-by-id labels:instance
CREATE INDEX idx_instance_admins_updated_by_id
    ON instance_admins(updated_by_id);
--rollback DROP INDEX IF EXISTS idx_instance_admins_updated_by_id;

--changeset syncturtle:0001-043-index-instance-admins-user-id labels:instance
CREATE INDEX idx_instance_admins_user_id
    ON instance_admins(user_id);
--rollback DROP INDEX IF EXISTS idx_instance_admins_user_id;

--changeset syncturtle:0001-044-index-instance-configurations-created-by-id labels:instance
CREATE INDEX idx_instance_configurations_created_by_id
    ON instance_configurations(created_by_id);
--rollback DROP INDEX IF EXISTS idx_instance_configurations_created_by_id;

--changeset syncturtle:0001-045-index-instance-configurations-key-like labels:instance
CREATE INDEX idx_instance_configurations_key_like
    ON instance_configurations USING btree (key varchar_pattern_ops);
--rollback DROP INDEX IF EXISTS idx_instance_configurations_key_like;

--changeset syncturtle:0001-046-index-instance-configurations-updated-by-id labels:instance
CREATE INDEX idx_instance_configurations_updated_by_id
    ON instance_configurations(updated_by_id);
--rollback DROP INDEX IF EXISTS idx_instance_configurations_updated_by_id;

--changeset syncturtle:0001-047-index-instances-created-by-id labels:instance
CREATE INDEX idx_instances_created_by_id
    ON instances(created_by_id);
--rollback DROP INDEX IF EXISTS idx_instances_created_by_id;

--changeset syncturtle:0001-048-index-instances-instance-id labels:instance
CREATE INDEX idx_instances_instance_id
    ON instances USING btree (instance_id varchar_pattern_ops);
--rollback DROP INDEX IF EXISTS idx_instances_instance_id;

--changeset syncturtle:0001-049-index-instances-updated-by-id labels:instance
CREATE INDEX idx_instances_updated_by_id
    ON instances(updated_by_id);
--rollback DROP INDEX IF EXISTS idx_instances_updated_by_id;