--liquibase formatted sql

--changeset syncturtle:0003-001-seed-system-user-lite labels:instance
INSERT INTO users_lite (
    id,
    username,
    email,
    display_name,
    first_name,
    last_name,
    user_timezone,
    principal_type,
    avatar_asset_id,
    cover_image_asset_id,
    is_active,
    is_email_verified,
    is_password_autoset,
    source_version,
    auth_version,
    created_at,
    updated_at,
    deleted_at,
    projected_at
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'system',
    'system@syncturtle.internal',
    'Syncturtle System',
    '',
    '',
    'UTC',
    'SYSTEM',
    NULL,
    NULL,
    TRUE,
    TRUE,
    TRUE,
    1,
    1,
    now(),
    now(),
    NULL,
    now()
)
ON CONFLICT (id) DO UPDATE
SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    display_name = EXCLUDED.display_name,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    user_timezone = EXCLUDED.user_timezone,
    principal_type = EXCLUDED.principal_type,
    is_active = EXCLUDED.is_active,
    is_email_verified = EXCLUDED.is_email_verified,
    is_password_autoset = EXCLUDED.is_password_autoset,
    source_version = GREATEST(users_lite.source_version, EXCLUDED.source_version),
    auth_version = GREATEST(users_lite.auth_version, EXCLUDED.auth_version),
    updated_at = now(),
    projected_at = now();

--rollback DELETE FROM users_lite WHERE id = '00000000-0000-0000-0000-000000000001';