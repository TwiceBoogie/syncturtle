--liquibase formatted sql

--changeset syncturtle:0001-040-index-instances-slug-active labels:instance
CREATE INDEX idx_instances_slug_active
    ON instances(slug)
    WHERE deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS idx_instances_slug_active;

--changeset syncturtle:0001-041-index-instances-instance-id labels:instance
CREATE INDEX idx_instances_instance_id
    ON instances(instance_id);
--rollback DROP INDEX IF EXISTS idx_instances_instance_id;

--changeset syncturtle:0001-050-ux-instance-admin-unique-active labels:instance
CREATE UNIQUE INDEX ux_instance_admin_unique_active
    ON instance_admins(instance_id, user_id)
    WHERE deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS ux_instance_admin_unique_active;

--changeset syncturtle:0001-051-index-instance-admin-user labels:instance
CREATE INDEX idx_instance_admin_user
    ON instance_admins(user_id);
--rollback DROP INDEX IF EXISTS idx_instance_admin_user;

--changeset syncturtle:0001-60-ux-users-lite-email-lower labels:instance
CREATE UNIQUE INDEX ux_users_lite_email_lower
    ON users_lite (lower(email));
--rollback DROP INDEX IF EXISTS ux_users_lite_email_lower;