--liquibase formatted sql

--changeset syncturtle:9000-001-dev-seed context:dev labels:seed
--rollback DELETE FROM instance_configurations WHERE key = 'DEV_WELCOME';

INSERT INTO instance_configurations (created_at, updated_at, id, key, value, category, is_encrypted)
VALUES (now(), now(), gen_random_uuid(), 'DEV_WELCOME', 'true', 'dev', false);