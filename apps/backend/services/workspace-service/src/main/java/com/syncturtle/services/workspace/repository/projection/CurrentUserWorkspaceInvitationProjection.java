package com.syncturtle.services.workspace.repository.projection;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.services.workspace.type.WorkspaceRole;

public interface CurrentUserWorkspaceInvitationProjection {
    UUID getId();

    String getEmail();

    boolean isAccepted();

    String getMessage();

    Instant getResponsedAt();

    WorkspaceRole getRole();

    String getToken();

    UUID getWorkspaceId();

    String getWorkspaceName();

    String getWorkspaceSlug();

    UUID getWorkspaceLogoAssetId();
}
