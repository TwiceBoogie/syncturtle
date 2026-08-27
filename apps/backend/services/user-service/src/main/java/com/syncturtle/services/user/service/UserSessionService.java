package com.syncturtle.services.user.service;

import java.util.UUID;

import com.syncturtle.services.user.dto.response.UserSessionInventoryResponse;
import com.syncturtle.services.user.dto.response.UserSessionRevocationResponse;

public interface UserSessionService {
    UserSessionInventoryResponse listSessions(UUID userId, UUID currentSessionId);

    UserSessionRevocationResponse revokeSession(UUID userId, UUID currentSessionId, UUID targetSessionId);

    UserSessionRevocationResponse revokeOtherSessions(UUID userId, UUID currentSessionId);

    UserSessionRevocationResponse revokeAllSessions(UUID userId, UUID currentSessionId);
}
