package com.syncturtle.services.user.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.model.Instance;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.RefreshSessionService;
import com.syncturtle.services.user.service.authz.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.authz.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.session.AuthenticatedSessionIssueSpec;
import com.syncturtle.services.user.service.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.session.RefreshSessionTokenStore;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshSessionServiceImpl implements RefreshSessionService {

    private final RefreshSessionTokenStore refreshSessionTokenStore;
    private final InstanceAuthorizationResolver authorizationResolver;
    private final AuthenticatedSessionIssuer authenticatedSessionIssuer;

    private final UserRepository userRepository;
    private final InstanceRepository instanceRepository;
    private final RequestClientContext clientContext;
    private final Clock clock;

    @Override
    @Transactional
    public IssueTokenResponse refreshSession(String presentedRefreshToken) {
        if (!StringUtils.hasText(presentedRefreshToken)) {
            throw invalidRefresh("Missing refresh token");
        }

        String sessionId = extractSessionId(presentedRefreshToken);

        RefreshSessionRecord session = refreshSessionTokenStore.findSession(sessionId)
                .orElseThrow(() -> invalidRefresh("Refresh session not found"));

        validateSessionState(sessionId, session, presentedRefreshToken);

        User user = requireRefreshUser(sessionId, session);
        Instance instance = requireRefreshInstance(sessionId, session);

        InstanceAuthorizationSnapshot authz = authorizationResolver.resolve(user.getId(), instance.getId());

        validateAdminState(sessionId, session, authz);

        return issueTokenResponse(user, instance.getId(), sessionId);
    }

    private void validateSessionState(String sessionId, RefreshSessionRecord session, String presentedRefreshToken) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.notNull(session, "refresh session is required");
        Assert.hasText(presentedRefreshToken, "presentedRefreshToken is required");

        if (!session.isActive()) {
            throw revokeAndInvalidRefresh(sessionId, "Refresh session expired");
        }

        Instant expiresAt = session.getExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(Instant.now(clock))) {
            throw revokeAndInvalidRefresh(sessionId, "Refresh session expired");
        }

        if (!refreshSessionTokenStore.matchesStoredRefreshToken(presentedRefreshToken, session)) {
            throw revokeAndInvalidRefresh(sessionId, "Refresh token mismatch");
        }
    }

    private User requireRefreshUser(String sessionId, RefreshSessionRecord session) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.notNull(session, "refresh session is required");

        UUID userId = parseRefreshUuid(sessionId, session.getUserId(), "userId");

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isLoginAllowed()) {
            throw revokeAndInvalidRefresh(sessionId, "User is missing or cannot login");
        }

        if (!Objects.equals(user.getAuthVersion(), session.getAuthVersion())) {
            throw revokeAndInvalidRefresh(sessionId, "Refresh session is stale");
        }

        return user;
    }

    private Instance requireRefreshInstance(String sessionId, RefreshSessionRecord session) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.notNull(session, "refresh session is required");

        UUID instanceId = parseRefreshUuid(sessionId, session.getInstanceId(), "instanceId");

        Instance instance = instanceRepository.findById(instanceId).orElse(null);
        if (instance == null || !instance.isSetupDone()) {
            throw revokeAndInvalidRefresh(sessionId, "Instance is not available");
        }

        return instance;
    }

    private void validateAdminState(String sessionId, RefreshSessionRecord session,
            InstanceAuthorizationSnapshot authz) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.notNull(session, "refresh session is required");
        Assert.notNull(authz, "authz is required");

        boolean sessionWasAdmin = session.getAdminSessionVersion() != null;
        if (!sessionWasAdmin) {
            return;
        }

        if (!authz.isInstanceAdmin()) {
            throw revokeAndInvalidRefresh(sessionId, "Admin access was revoked");
        }

        if (!Objects.equals(session.getAdminSessionVersion(), authz.getAdminSessionVersion())) {
            throw revokeAndInvalidRefresh(sessionId, "Admin session version is stale");
        }
    }

    private String extractSessionId(String presentedRefreshToken) {
        Assert.hasText(presentedRefreshToken, "presentedRefreshToken is required");

        try {
            return refreshSessionTokenStore.extractSessionIdFromRefreshToken(presentedRefreshToken);
        } catch (IllegalArgumentException exception) {
            throw invalidRefresh("Invalid refresh token format");
        }
    }

    private UUID parseRefreshUuid(String sessionId, String value, String fieldName) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.hasText(fieldName, "fieldName is required");

        if (!StringUtils.hasText(value)) {
            throw revokeAndInvalidRefresh(sessionId, "Refresh session is missing " + fieldName);
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw revokeAndInvalidRefresh(sessionId, "Refresh session has invalid " + fieldName);
        }
    }

    private AuthException revokeAndInvalidRefresh(String sessionId, String message) {
        if (StringUtils.hasText(sessionId)) {
            refreshSessionTokenStore.revokeSession(sessionId);
        }

        return invalidRefresh(message);
    }

    private AuthException invalidRefresh(String message) {
        return AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED)
                .with("message", message);
    }

    private IssueTokenResponse issueTokenResponse(User user, UUID instanceId, String sessionId) {
        Assert.notNull(user, "user is required");
        Assert.notNull(instanceId, "instanceId is required");
        Assert.hasText(sessionId, "sessionId is required");

        AuthenticatedSessionReceipt session = authenticatedSessionIssuer.rotateSession(
                AuthenticatedSessionIssueSpec.builder()
                        .user(user)
                        .instanceId(instanceId)
                        .ipAddress(clientContext.getClientIp())
                        .userAgent(clientContext.getUserAgent())
                        .build(),
                sessionId);

        return IssueTokenResponse.issued(session, null);
    }

}
