package com.syncturtle.services.workspace.dto.response;

import java.util.UUID;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkspaceMemberInvitationResponse {
    UUID id;
    String email;
    boolean accepted;
    String message;
    String token;
    String inviteLink;
    WorkspaceRole role;
    WorkspaceInvitationWorkspaceResponse workspace;
}
