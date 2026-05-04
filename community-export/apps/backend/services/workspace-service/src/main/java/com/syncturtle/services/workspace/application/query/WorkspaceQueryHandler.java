package com.syncturtle.services.workspace.application.query;

import org.springframework.stereotype.Component;

import com.syncturtle.common.web.pagination.CursorPageResponse;
import com.syncturtle.services.workspace.controllers.mappers.WorkspaceApiMapper;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.services.workspace.services.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceQueryHandler {

    private final WorkspaceService workspaceService;
    private final WorkspaceApiMapper mapper;

    public CursorPageResponse<WorkspaceResponse> workspaceAll(String cursor, int perPage, String search) {
        return mapper.toCursorPageWorkspaceResponse(workspaceService.workspaceAll(cursor, perPage, search), perPage);
    }

    public WorkspaceSlugCheckResponse workspaceSlugCheck(String slug) {
        return workspaceService.workspaceSlugCheck(slug);
    }

    public WorkspaceResponse workspaceCreate(WorkspaceCreateRequest request) {
        return mapper.toWorkspaceResponse(workspaceService.workspaceCreate(request));
    }

}
