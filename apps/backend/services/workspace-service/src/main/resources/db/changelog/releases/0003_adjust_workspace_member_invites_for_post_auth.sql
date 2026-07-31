--liquibase formatted sql

--changeset syncturtle:0003-001-adjust-workspace-member-invites-for-post-auth labels:workspace
ALTER TABLE workspace_member_invites
    ADD COLUMN IF NOT EXISTS consumed_at TIMESTAMPTZ;
--rollback ALTER TABLE workspace_member_invites DROP COLUMN IF EXISTS consumed_at;