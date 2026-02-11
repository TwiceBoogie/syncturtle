--liquibase formatted sql

--changeset syncturtle:0001-040-index-workspaces-created-by-id labels:workspace
CREATE INDEX idx_workspaces_created_by_id
    ON workspaces(created_by_id);
--rollback DROP INDEX IF EXISTS idx_workspaces_created_by_id;

--changeset syncturtle:0001-041-index-workspaces-owner-id labels:workspace
CREATE INDEX idx_workspaces_owner_id
    ON workspaces(owner_id);
--rollback DROP INDEX IF EXISTS idx_workspaces_owner_id;

--changeset syncturtle:0001-042-index-workspaces-slug-like labels:workspace
CREATE INDEX idx_workspaces_slug_like
    ON workspaces(slug varchar_pattern_ops);
--rollback DROP INDEX IF EXISTS idx_workspaces_slug_like;

--changeset syncturtle:0001-043-index-workspaces-updated-by-id labels:workspace
CREATE INDEX idx_workspaces_updated_by_id
    ON workspaces(updated_by_id);
--rollback DROP INDEX IF EXISTS idx_workspaces_updated_by_id;

--changeset syncturtle:0001-044-index-workspaces-logo-asset-id labels:workspace
CREATE INDEX idx_workspaces_logo_asset_id
    ON workspaces(logo_asset_id);
--rollback DROP INDEX IF EXISTS idx_workspaces_logo_asset_id;

--changeset syncturtle:0001-050-index-workspace-member-invites-created-by-id labels:workspace
CREATE INDEX idx_workspace_member_invites_created_by_id
    ON workspace_member_invites(created_by_id);
--rollback DROP INDEX IF EXISTS idx_workspace_member_invites_created_by_id;

--changeset syncturtle:0001-051-unique-index-workspace-member-invites-email-workspace-when-deleted-at-null labels:workspace
CREATE UNIQUE INDEX uq_idx_workspace_member_invites_workspace_email_when_deleted_at_null
    ON workspace_member_invites(workspace_id, lower(email))
    WHERE deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS uq_idx_workspace_member_invites_email_workspace_when_deleted_at_null;

--changeset syncturtle:0001-052-index-workspace-member-invites-updated-by-id labels:workspace
CREATE INDEX idx_workspace_member_invites_updated_by_id
    ON workspace_member_invites(updated_by_id);
--rollback DROP INDEX IF EXISTS idx_workspace_member_invites_updated_by_id;

--changeset syncturtle:0001-053-index-workspace-member-invites-workspace-id labels:workspace
CREATE INDEX idx_workspace_member_invites_workspace_id
    ON workspace_member_invites(workspace_id);
--rollback DROP INDEX IF EXISTS idx_workspace_member_invites_workspace_id;

--changeset syncturtle:0001-060-index-workspace-members-created-by-id labels:workspace
CREATE INDEX idx_workspace_members_created_by_id
    ON workspace_members(created_by_id);
--rollback DROP INDEX IF EXISTS idx_workspace_members_created_by_id;

--changeset syncturtle:0001-061-index-workspace-members-updated-by-id labels:workspace
CREATE INDEX idx_workspace_members_updated_by_id
    ON workspace_members(updated_by_id);
--rollback DROP INDEX IF EXISTS idx_workspace_members_updated_by_id;

--changeset syncturtle:0001-062-index-workspace-members-member-id labels:workspace
CREATE INDEX idx_workspace_members_member_id
    ON workspace_members(member_id);
--rollback DROP INDEX IF EXISTS idx_workspace_members_member_id;

--changeset syncturtle:0001-063-index-workspace-members-workspace-id labels:workspace
CREATE INDEX idx_workspace_members_workspace_id
    ON workspace_members(workspace_id);
--rollback DROP INDEX IF EXISTS idx_workspace_members_workspace_id;

--changeset syncturtle:0001-064-unique-index-workspace-members-workspace-member-when-deleted-at-null labels:workspace
CREATE UNIQUE INDEX uq_idx_workspace_members_workspace_member_when_deleted_at_null
    ON workspace_members(workspace_id, member_id)
    WHERE deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS uq_idx_workspace_members_workspace_member_when_deleted_at_null;