package com.syncturtle.services.user.service.impl;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.exception.RefreshSessionLifecycleException;
import com.syncturtle.services.user.model.InstanceLite;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceLiteRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.RefreshSessionService;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionFamilyStore;
import com.syncturtle.services.user.service.param.AuthenticatedSessionIssueParam;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshSessionServiceImpl implements RefreshSessionService {

    private final RefreshSessionFamilyStore refreshSessionFamilyStore;
    private final InstanceAuthorizationResolver authorizationResolver;
    private final AuthenticatedSessionIssuer authenticatedSessionIssuer;
    private final UserRepository userRepository;
    private final InstanceLiteRepository instanceRepository;
    private final RequestClientContext clientContext;

    @Override
    @Transactional
    public IssueTokenResponse refreshSession(String presentedRefreshToken) {
        if (!StringUtils.hasText(presentedRefreshToken)) {
            throw invalidRefresh("Missing refresh token");
        }

        String sessionId = extractSessionId(presentedRefreshToken);
        RefreshSessionFamilyRecord family = requireFamily(sessionId);
        User user = requireRefreshUser(sessionId, family);
        InstanceLite instance = requireRefreshInstance(sessionId, family);
        InstanceAuthorizationSnapshot authorization = authorizationResolver.resolve(user.getId(), instance.getId());
        validateAdminState(sessionId, family, authorization);

        try {
            AuthenticatedSessionReceipt session = authenticatedSessionIssuer.rotateSession(
                    AuthenticatedSessionIssueParam.builder()
                            .user(user)
                            .instanceId(instance.getId())
                            .ipAddress(clientContext.getClientIp())
                            .userAgent(clientContext.getUserAgent())
                            .build(),
                    presentedRefreshToken,
                    authorization);
            return IssueTokenResponse.issued(session, null);
        } catch (RefreshSessionLifecycleException exception) {
            throw mapLifecycleFailure(exception);
        }
    }

    private String extractSessionId(String presentedRefreshToken) {
        Assert.hasText(presentedRefreshToken, "presentedRefreshToken is required");

        try {
            return refreshSessionFamilyStore.extractSessionId(presentedRefreshToken);
        } catch (IllegalArgumentException exception) {
            throw invalidRefresh("Invalid refresh token format");
        }
    }

    private RefreshSessionFamilyRecord requireFamily(String sessionId) {
        try {
            return refreshSessionFamilyStore.requireFamily(sessionId);
        } catch (RefreshSessionLifecycleException exception) {
            throw mapLifecycleFailure(exception);
        }
    }

    private User requireRefreshUser(String sessionId, RefreshSessionFamilyRecord family) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.notNull(family, "refresh session family is required");

        UUID userId = UUID.fromString(family.getUserId());
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isLoginAllowed()) {
            throw revokeAndInvalidRefresh(sessionId, family.getUserId(),
                    "User is missing or cannot login");
        }
        if (!Objects.equals(user.getAuthVersion(), family.getAuthVersion())) {
            throw revokeAndInvalidRefresh(sessionId, family.getUserId(),
                    "Refresh session is stale");
        }
        return user;
    }

    private InstanceLite requireRefreshInstance(String sessionId, RefreshSessionFamilyRecord family) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.notNull(family, "refresh session family is required");

        UUID instanceId = UUID.fromString(family.getInstanceId());
        InstanceLite instance = instanceRepository.findById(instanceId).orElse(null);
        if (instance == null || !instance.isSetupDone()) {
            throw revokeAndInvalidRefresh(sessionId, family.getUserId(),
                    "Instance is not available");
        }
        return instance;
    }

    private void validateAdminState(
            String sessionId,
            RefreshSessionFamilyRecord family,
            InstanceAuthorizationSnapshot authorization) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.notNull(family, "refresh session family is required");
        Assert.notNull(authorization, "authorization is required");

        if (family.getAdminSessionVersion() == null) {
            return;
        }
        if (!authorization.isInstanceAdmin()) {
            throw revokeAndInvalidRefresh(sessionId, family.getUserId(),
                    "Admin access was revoked");
        }
        if (!Objects.equals(family.getAdminSessionVersion(), authorization.getAdminSessionVersion())) {
            throw revokeAndInvalidRefresh(sessionId, family.getUserId(),
                    "Admin session version is stale");
        }
    }

    private AuthException revokeAndInvalidRefresh(String sessionId, String userId, String message) {
        try {
            refreshSessionFamilyStore.revokeOne(userId, sessionId);
        } catch (RefreshSessionLifecycleException exception) {
            throw mapLifecycleFailure(exception);
        }
        return invalidRefresh(message);
    }

    private RuntimeException mapLifecycleFailure(RefreshSessionLifecycleException exception) {
        if (exception.getReason() == RefreshSessionLifecycleException.Reason.REDIS_FAILURE
                || exception.getReason() == RefreshSessionLifecycleException.Reason.INVARIANT_VIOLATION) {
            return RemoteServiceException.unavailable("session-state", exception);
        }
        return invalidRefresh("Refresh session is no longer valid");
    }

    private static AuthException invalidRefresh(String message) {
        return AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED)
                .with("message", message);
    }

}
