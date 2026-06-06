--liquibase formatted sql

--changeset syncturtle:0001-005-adjust-users-for-principals labels:user
ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT users_principal_type_ck
    CHECK (principal_type IN ('HUMAN', 'SYSTEM', 'BOT', 'SERVICE_ACCOUNT'));

--rollback ALTER TABLE users DROP CONSTRAINT IF EXISTS users_principal_type_ck;
--rollback ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;
--rollback ALTER TABLE users DROP COLUMN IF EXISTS updated_by_id;
--rollback ALTER TABLE users DROP COLUMN IF EXISTS created_by_id;
--rollback ALTER TABLE users DROP COLUMN IF EXISTS deleted_at;