package com.syncturtle.services.workspace.repository.query;

public final class CurrentUserWorkspaceSql {

    public static final String SELECT_CURRENT_USER_WORKSPACE = """
            SELECT
                workspace.id AS "id",
                workspace.name AS "name",
                workspace.slug AS "slug",
                member.role AS "role",
                workspace.logo_asset_id AS "logoAssetId",
                workspace.organization_size AS "organizationSize",
                workspace.owner_id AS "ownerId",
                owner.avatar_asset_id AS "ownerAvatarAssetId",
                owner.display_name AS "ownerDisplayName",
                owner.email AS "ownerEmail",
                owner.first_name AS "ownerFirstName",
                owner.last_name AS "ownerLastName",
                owner.principal_type AS "ownerPrincipalType",
                owner.created_at AS "ownerCreatedAt",
                workspace.created_by_id AS "createdById",
                workspace.updated_by_id AS "updatedById",
                workspace.created_at AS "createdAt",
                workspace.updated_at AS "updatedAt",
                (
                    SELECT COUNT(active_member.id)
                    FROM workspace_members active_member
                    LEFT JOIN users_lite active_user
                        ON active_user.id = active_member.member_id
                    WHERE active_member.workspace_id = workspace.id
                        AND active_member.is_active = true
                        AND active_member.deleted_at IS NULL
                        AND (
                                active_user.id IS NULL
                                OR active_user.principal_type <> 'BOT'
                            )
                ) AS "totalMembers"
            """;

    public static final String FROM_CURRENT_USER_WORKSPACE = """
            FROM workspace_members member
            JOIN workspaces workspace
                ON workspace.id = member.workspace_id
            LEFT JOIN users_lite owner
                ON owner.id = workspace.owner_id
            """;

    public static final String WHERE_CURRENT_USER_CAN_ACCESS_WORKSPACE = """
            WHERE member.member_id = :currentUserId
                AND member.is_active = true
                AND member.deleted_at IS NULL
                AND workspace.deleted_at IS NULL
            """;

    public static final String FIND_CURRENT_USER_WORKSPACES = SELECT_CURRENT_USER_WORKSPACE
            + FROM_CURRENT_USER_WORKSPACE
            + WHERE_CURRENT_USER_CAN_ACCESS_WORKSPACE
            + "ORDER BY workspace.name ASC";

    public static final String FIND_CURRENT_USER_WORKSPACE_BY_ID = SELECT_CURRENT_USER_WORKSPACE
            + FROM_CURRENT_USER_WORKSPACE
            + WHERE_CURRENT_USER_CAN_ACCESS_WORKSPACE
            + "AND workspace.id = :workspaceId";

    private CurrentUserWorkspaceSql() {
    }

}
