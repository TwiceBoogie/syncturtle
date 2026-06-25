package com.syncturtle.services.user.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserMeSettingsWorkspaceResponse {
    UUID lastWorkspaceId;
    String lastWorkspaceSlug;
    String lastWorkspaceName;
    String lastWorkspaceLogo;
    UUID fallbackWorkspaceId;
    String fallbackWorkspaceSlug;
    long invites;
}
