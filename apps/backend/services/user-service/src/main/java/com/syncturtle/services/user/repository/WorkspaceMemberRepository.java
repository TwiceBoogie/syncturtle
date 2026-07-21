package com.syncturtle.services.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.syncturtle.services.user.model.WorkspaceMemberLite;
import com.syncturtle.services.user.repository.projection.UserSettingsWorkspaceProjection;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMemberLite, UUID> {
    @Query("""
            SELECT
                workspace.id AS id,
                workspace.slug AS slug,
                workspace.name AS name,
                workspace.logoAssetId AS logoAssetId
            FROM WorkspaceMemberLite member
            JOIN member.workspace workspace
            WHERE member.memberId = :userId
                AND member.workspaceId = :workspaceId
                AND member.active = true
                AND member.deletedAt IS NULL
                AND workspace.deletedAt IS NULL
            """)
    Optional<UserSettingsWorkspaceProjection> findActiveWorkspaceForUser(UUID userId, UUID workspaceId);

    @Query("""
            SELECT
                workspace.id AS id,
                workspace.slug AS slug,
                workspace.name AS name,
                workspace.logoAssetId AS logoAssetId
            FROM WorkspaceMemberLite member
            JOIN member.workspace workspace
            WHERE member.memberId = :userId
                AND member.active = true
                AND member.deletedAt IS NULL
                AND workspace.deletedAt IS NULL
            ORDER BY workspace.createdAt ASC
            LIMIT 1
            """)
    Optional<UserSettingsWorkspaceProjection> findFirstActiveWorkspaceForUser(UUID userId);
}
