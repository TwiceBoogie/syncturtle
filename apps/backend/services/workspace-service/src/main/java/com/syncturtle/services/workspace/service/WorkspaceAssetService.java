package com.syncturtle.services.workspace.service;

import java.util.UUID;

import com.syncturtle.services.workspace.dto.request.WorkspaceLogoUpdateRequest;

public interface WorkspaceAssetService {
    void updateLogo(UUID currentUserId, String workspaceSlug, WorkspaceLogoUpdateRequest request);

    void clearLogo(UUID currentUserId, String workspaceSlug);
}
