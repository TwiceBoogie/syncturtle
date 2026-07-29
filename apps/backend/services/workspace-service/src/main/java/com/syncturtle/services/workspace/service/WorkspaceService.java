package com.syncturtle.services.workspace.service;

import java.util.List;
import java.util.UUID;

import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.request.WorkspaceInvitationRequest;
import com.syncturtle.services.workspace.dto.response.SimpleMessageResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceMemberInvitationResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;

public interface WorkspaceService {
    WorkspaceSlugCheckResponse workspaceSlugCheck(String slug);

    WorkspaceResponse workspaceCreate(UUID currentUserId, WorkspaceCreateRequest request);

    List<WorkspaceResponse> getCurrentUserWorkspaces(UUID currentUserId);

    List<WorkspaceMemberInvitationResponse> listCurrentUserInvitations(UUID currentUserId);

    SimpleMessageResponse createWorkspaceInvitations(UUID currentUserId, String workspaceSlug,
            WorkspaceInvitationRequest request);
}
