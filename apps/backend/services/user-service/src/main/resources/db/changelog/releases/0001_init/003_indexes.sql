--liquibase formatted sql

--changeset syncturtle:0001-020-index-users-email-like labels:users
CREATE INDEX idx_users_email_like
    ON users USING btree (email varchar_pattern_ops);
--rollback DROP INDEX IF EXISTS idx_users_email_like;

--changeset syncturtle:0001-021-index-users-username-like labels:users
CREATE INDEX idx_users_username_like
    ON users USING btree (username varchar_pattern_ops);
--rollback DROP INDEX IF EXISTS idx_users_username_like;

--changeset syncturtle:0001-022-index-accounts-user-id labels:users
CREATE INDEX idx_accounts_user_id
    ON accounts(user_id);
--rollback DROP INDEX IF EXISTS idx_accounts_user_id;