package com.syncturtle.services.workspace.service;

import java.util.UUID;

import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;

public interface WorkspaceAdministrationInternalService {
    WorkspaceSlugCheckResponse checkSlug(UUID currentUserId, String slug);

    WorkspaceResponse createWorkspace(UUID currentUserId,
            WorkspaceCreateRequest request);
}
