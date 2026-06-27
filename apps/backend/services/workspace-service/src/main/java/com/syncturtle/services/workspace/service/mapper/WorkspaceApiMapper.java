package com.syncturtle.services.workspace.service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.web.pagination.CursorPageResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceMemberInvitationResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceInvitationProjection;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class WorkspaceApiMapper {

    private final WorkspaceResponseMapper workspaceResponseMapper;
    private final WorkspacePageMapper workspacePageMapper;

    public CursorPageResponse<WorkspaceResponse> toCursorPageWorkspaceResponse(
            List<CurrentUserWorkspaceProjection> workspaces,
            int perPage) {
        Assert.notNull(workspaces, "workspaces is required");

        return workspacePageMapper.mapToPageResponse(workspaces, perPage);
    }

    public WorkspaceResponse toWorkspaceResponse(CurrentUserWorkspaceProjection projection) {
        return workspaceResponseMapper.toResponse(projection);
    }

    public WorkspaceMemberInvitationResponse toWorkspaceMemberInvitationResponse(
            CurrentUserWorkspaceInvitationProjection projection) {
        return workspaceResponseMapper.toWorkspaceMemberInvitationResponse(projection);
    }

}
