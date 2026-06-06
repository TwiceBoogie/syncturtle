package com.syncturtle.services.instance.service;

import java.util.UUID;

import com.syncturtle.common.web.pagination.CursorPageResponse;
import com.syncturtle.services.instance.dto.request.InstanceWorkspaceCreateRequest;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceResponse;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceSlugCheckResponse;

public interface InstanceWorkspaceAdminService {
    InstanceWorkspaceSlugCheckResponse checkSlug(UUID currentUserId, String slug);

    CursorPageResponse<InstanceWorkspaceResponse> getWorkspaces(UUID currentUserId, String cursor, int perPage,
            String search);

    InstanceWorkspaceResponse createWorkspace(UUID currentUserId, InstanceWorkspaceCreateRequest request);
}
