package com.syncturtle.services.workspace.repository;

import static com.syncturtle.services.workspace.repository.query.CurrentUserWorkspaceSql.FIND_CURRENT_USER_WORKSPACES;
import static com.syncturtle.services.workspace.repository.query.CurrentUserWorkspaceSql.FIND_CURRENT_USER_WORKSPACE_BY_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceProjection;
import com.syncturtle.services.workspace.type.WorkspaceRole;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    long countByWorkspace_Id(UUID workspaceId);

    @Query(value = FIND_CURRENT_USER_WORKSPACES, nativeQuery = true)
    List<CurrentUserWorkspaceProjection> findCurrentUserWorkspaces(@Param("currentUserId") UUID currentUserId);

    @Query(value = FIND_CURRENT_USER_WORKSPACE_BY_ID, nativeQuery = true)
    Optional<CurrentUserWorkspaceProjection> findCurrentUserWorkspaceById(@Param("currentUserId") UUID currentUserId,
            @Param("workspaceId") UUID workspaceId);

    boolean existsByMemberIdAndWorkspace_IdAndRoleAndDeletedAtIsNull(UUID memberId, UUID workspaceId,
            WorkspaceRole role);

    long countByWorkspace_IdAndDeletedAtIsNull(UUID workspaceId);
}
