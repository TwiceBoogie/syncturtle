package com.syncturtle.services.workspace.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.syncturtle.services.workspace.model.WorkspaceMemberInvite;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceInvitationProjection;

public interface WorkspaceMemberInviteRepository extends JpaRepository<WorkspaceMemberInvite, UUID> {

    @Query("""
            SELECT
                invite.id AS id,
                invite.email AS email,
                invite.accepted AS accepted,
                invite.message AS message,
                invite.respondedAt AS respondedAt,
                invite.role AS role,
                invite.token AS token,
                workspace.id AS workspaceId,
                workspace.name AS workspaceName,
                workspace.slug AS workspaceSlug,
                workspace.logoAssetId AS workspaceLogoAssetId
            FROM WorkspaceMemberInvite invite
            JOIN invite.workspace workspace
            WHERE LOWER(invite.email) = LOWER(:email)
                AND invite.deletedAt IS NULL
                AND workspace.deletedAt IS NULL
            ORDER BY invite.createdAt DESC
            """)
    List<CurrentUserWorkspaceInvitationProjection> findCurrentUserInvitationsByEmail(String email);

    @Query("""
            SELECT LOWER(invitation.email)
            FROM WorkspaceMemberInvite invitation
            WHERE invitation.workspace.id = :workspaceId
                AND LOWER(invitation.email) in :emails
                AND invitation.accepted = false
            """)
    List<String> findPendingInvitationEmailsByWorkspaceIdAndEmails(UUID workspaceId, Collection<String> emails);

    List<WorkspaceMemberInvite> findAllByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<WorkspaceMemberInvite> findAllByIdInAndEmailIgnoreCaseOrderByCreatedAtDesc(Collection<UUID> ids, String email);

    @EntityGraph(attributePaths = "workspace")
    List<WorkspaceMemberInvite> findAllByEmailIgnoreCaseAndAcceptedTrueAndRespondedAtIsNotNullAndConsumedAtIsNullAndDeletedAtIsNull(
            String email);

    Optional<WorkspaceMemberInvite> findByTokenAndDeletedAtIsNull(String token);
}
