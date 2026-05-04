--liquibase formatted sql

--changeset syncturtle:0001-010-unique-workspaces-slug labels:workspace
ALTER TABLE workspaces
    ADD CONSTRAINT uq_workspaces_slug UNIQUE (slug);
--rollback ALTER TABLE workspaces DROP CONSTRAINT IF EXISTS uq_workspaces_slug;

--changeset syncturtle:0001-015-fk-workspace-member-invites-workspace labels:workspace
ALTER TABLE workspace_member_invites
    ADD CONSTRAINT fk_workspace_member_invites_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
    DEFERRABLE INITIALLY DEFERRED;
--rollback ALTER TABLE workspace_member_invites DROP CONSTRAINT IF EXISTS fk_workspace_member_invites_workspace;

--changeset syncturtle:0001-016-check-workspace-member-invites-role-non-negative labels:workspace
ALTER TABLE workspace_member_invites
    ADD CONSTRAINT ck_workspace_member_invites_role_non_negative
    CHECK (role >= 0);
--rollback ALTER TABLE workspace_member_invites DROP CONSTRAINT IF EXISTS ck_workspace_member_invites_role_non_negative;

--changeset syncturtle:0001-020-fk-workspace-members-workspace labels:workspace
ALTER TABLE workspace_members
    ADD CONSTRAINT fk_workspace_members_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
    DEFERRABLE INITIALLY DEFERRED;
--rollback ALTER TABLE workspace_members DROP CONSTRAINT IF EXISTS fk_workspace_members_workspace;