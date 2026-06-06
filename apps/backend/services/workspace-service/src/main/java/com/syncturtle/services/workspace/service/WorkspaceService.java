package com.syncturtle.services.workspace.service;

import java.util.UUID;

import com.syncturtle.common.web.pagination.CursorPageResponse;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;

public interface WorkspaceService {
    CursorPageResponse<WorkspaceResponse> workspaceAll(UUID currentUserId, String cursor, int perPage, String search);

    WorkspaceSlugCheckResponse workspaceSlugCheck(String slug);

    WorkspaceResponse workspaceCreate(UUID currentUserId, WorkspaceCreateRequest request);
}
