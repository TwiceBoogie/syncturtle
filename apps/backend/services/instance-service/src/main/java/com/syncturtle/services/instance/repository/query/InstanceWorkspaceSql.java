package com.syncturtle.services.instance.repository.query;

public final class InstanceWorkspaceSql {

    public static final String FIND_WORKSPACE_PAGE_DESC = """
            WITH page AS (
                SELECT
                    workspace.id,
                    workspace.name,
                    workspace.logo_asset_id,
                    workspace.slug,
                    workspace.organization_size,
                    workspace.owner_id,
                    workspace.timezone,
                    workspace.created_by_id,
                    workspace.updated_by_id,
                    workspace.created_at,
                    workspace.updated_at
                FROM workspaces_lite workspace
                WHERE workspace.deleted_at IS NULL
                    AND (
                        CAST(:pattern AS text) IS NULL
                        OR LOWER(workspace.name) LIKE :pattern ESCAPE '!'
                        OR LOWER(workspace.slug) LIKE :pattern ESCAPE '!'
                    )
                    AND (
                        CAST(:cursorCreatedAt AS timestamp with time zone) IS NULL
                        OR CAST(:cursorId AS uuid) IS NULL
                        OR workspace.created_at < :cursorCreatedAt
                        OR (
                            workspace.created_at = :cursorCreatedAt
                            AND workspace.id < :cursorId
                        )
                    )
                ORDER BY workspace.created_at DESC, workspace.id DESC
                LIMIT :limit
            )
            SELECT
                page.id AS "id",
                page.name AS "name",
                page.logo_asset_id AS "logoAssetId",
                page.slug AS "slug",
                page.organization_size AS "organizationSize",
                page.timezone AS "timezone",
                page.created_by_id AS "createdById",
                page.updated_by_id AS "updatedById",
                page.created_at AS "createdAt",
                page.updated_at AS "updatedAt",

                COALESCE(member_count.total_members, 0) AS "totalMembers",

                owner.id AS "ownerId",
                owner.username AS "ownerUsername",
                owner.email AS "ownerEmail",
                owner.display_name AS "ownerDisplayName",
                owner.first_name AS "ownerFirstName",
                owner.last_name AS "ownerLastName",
                owner.created_at AS "ownerCreatedAt",
                owner.avatar_asset_id AS "ownerAvatarAssetId",
                owner.cover_image_asset_id AS "ownerCoverImageAssetId",
                owner.is_email_verified AS "ownerEmailVerified",
                owner.is_password_autoset AS "ownerPasswordAutoset",
                owner.user_timezone AS "ownerUserTimezone",
                owner.principal_type AS "ownerPrincipalType"
            FROM page
            LEFT JOIN users_lite owner
                ON owner.id = page.owner_id
                AND owner.deleted_at IS NULL
                AND owner.is_active = true
            LEFT JOIN LATERAL (
                SELECT COUNT(active_member.id) AS total_members
                FROM workspace_members_lite active_member
                LEFT JOIN users_lite active_user
                    ON active_user.id = active_member.member_id
                WHERE active_member.workspace_id = page.id
                    AND active_member.is_active = true
                    AND active_member.deleted_at IS NULL
                    AND (
                        active_user.id IS NULL
                        OR active_user.principal_type <> 'BOT'
                    )
            ) member_count ON true
            ORDER BY page.created_at DESC, page.id DESC
            """;

    private InstanceWorkspaceSql() {
    }

}
