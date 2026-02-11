package com.syncturtle.platform.services.workspace.services;

import java.util.List;

import com.syncturtle.platform.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.platform.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.platform.services.workspace.repositories.projections.WorkspaceProjection;

public interface WorkspaceService {
    List<WorkspaceProjection> workspaceAll(String cursor, int perPage, String search);

    WorkspaceSlugCheckResponse workspaceSlugCheck(String slug);

    WorkspaceProjection workspaceCreate(WorkspaceCreateRequest request);
}
