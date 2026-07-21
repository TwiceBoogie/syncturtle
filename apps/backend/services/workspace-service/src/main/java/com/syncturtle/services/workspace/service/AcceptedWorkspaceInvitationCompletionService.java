package com.syncturtle.services.workspace.service;

import java.util.UUID;

public interface AcceptedWorkspaceInvitationCompletionService {
    void completeAcceptedInvitations(UUID userId, String email);
}
