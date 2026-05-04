package com.syncturtle.services.user.services;

import java.util.Optional;

import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.services.user.dto.command.CreateRefreshSessionCommand;
import com.syncturtle.services.user.dto.command.RotateRefreshSessionCommand;
import com.syncturtle.services.user.payload.IssuedRefreshToken;

public interface RefreshTokenService {
    IssuedRefreshToken createSessionRefreshToken(CreateRefreshSessionCommand command);

    Optional<RefreshSessionRecord> findSession(String sessionId);

    IssuedRefreshToken rotateRefreshToken(RotateRefreshSessionCommand command);

    void revokeSession(String sessionId);

    String extractSessionIdFromRefreshToken(String refreshToken);

    boolean matchesStoredRefreshToken(String presentedRefreshToken, RefreshSessionRecord record);

    String sessionKey(String sessionId);

    String currentUserAuthVersionKey(String userId);

    String currentAdminSessionVersionKey(String instanceId, String userId);
}
