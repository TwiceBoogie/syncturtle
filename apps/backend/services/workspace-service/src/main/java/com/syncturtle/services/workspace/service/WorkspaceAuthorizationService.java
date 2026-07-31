package com.syncturtle.services.workspace.service;

import java.util.UUID;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;

public interface WorkspaceAuthorizationService {
    boolean hasWorkspaceRoleAtLeast(UUID userId, String workspaceSlug, WorkspaceRole requiredRole);

    boolean isActiveWorkspaceMember(UUID userId, String workspaceSlug);
}
