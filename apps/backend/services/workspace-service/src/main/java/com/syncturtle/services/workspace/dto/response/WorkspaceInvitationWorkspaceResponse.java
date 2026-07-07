package com.syncturtle.services.workspace.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkspaceInvitationWorkspaceResponse {
    UUID id;
    String name;
    String slug;
    String logoUrl;
}
