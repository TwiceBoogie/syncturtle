--liquibase formatted sql

--changeset syncturtle:0001-010-unique-users-email labels:users
ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email);
--rollback ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_email;

--changeset syncturtle:0001-011-unique-users-username labels:users
ALTER TABLE users
    ADD CONSTRAINT uq_users_username UNIQUE (username);
--rollback ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_username;

--changeset syncturtle:0001-012-unique-profiles-user-id labels:users
ALTER TABLE profiles
    ADD CONSTRAINT uq_profiles_user_id UNIQUE (user_id);
--rollback ALTER TABLE profiles DROP CONSTRAINT IF EXISTS uq_profiles_user_id;

--changeset syncturtle:0001-013-fk-profiles-user-id labels:users
ALTER TABLE profiles
    ADD CONSTRAINT fk_profiles_users
    FOREIGN KEY (user_id) REFERENCES users(id)
    DEFERRABLE INITIALLY DEFERRED;
--rollback ALTER TABLE profiles DROP CONSTRAINT IF EXISTS fk_profiles_users;

--changeset syncturtle:0001-014-unique-accounts-provider-account-id-provider label:users
ALTER TABLE accounts
    ADD CONSTRAINT uq_account_provider_account_id_provider UNIQUE (provider_account_id, provider);
--rollback ALTER TABLE accounts DROP CONSTRAINT IF EXISTS uq_account_provider_account_id_provider;

--changeset syncturtle:0001-015-fk-accounts-users labels:users
ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_users
    FOREIGN KEY (user_id) REFERENCES users(id)
    DEFERRABLE INITIALLY DEFERRED;
--rollback ALTER TABLE accounts DROP CONSTRAINT IF EXISTS fk_accounts_users;