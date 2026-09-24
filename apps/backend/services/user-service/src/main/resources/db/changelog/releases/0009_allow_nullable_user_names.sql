--liquibase formatted sql

--changeset syncturtle:0009-001-allow-nullable-user-names labels:user
ALTER TABLE users
    ALTER COLUMN first_name DROP NOT NULL,
    ALTER COLUMN last_name DROP NOT NULL;

--rollback UPDATE users SET first_name = '' WHERE first_name IS NULL;
--rollback UPDATE users SET last_name = '' WHERE last_name IS NULL;
--rollback ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
--rollback ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;
