package com.syncturtle.services.user.service.collaborator.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.exception.RefreshSessionException;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.token.AccessTokenIssuer;
import com.syncturtle.services.user.service.collaborator.token.IssuedAccessTokenReceipt;
import com.syncturtle.services.user.service.param.AccessTokenIssueParam;
import com.syncturtle.services.user.service.param.AuthenticatedSessionIssueParam;
import com.syncturtle.services.user.service.param.AuthorizedSessionIssueParam;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticatedSessionIssuer {

    private static final String INSTANCE_ADMIN_ROLE = "INSTANCE_ADMIN";

    private final RefreshSessionStore refreshSessionStore;
    private final AccessTokenIssuer accessTokenIssuer;
    private final InstanceAuthorizationResolver authorizationResolver;

    public AuthenticatedSessionReceipt issueNewSession(AuthenticatedSessionIssueParam param) {
        Assert.notNull(param, "param is required");

        User user = param.getUser();
        UUID instanceId = param.getInstanceId();
        validatePrincipal(user, instanceId);

        InstanceAuthorizationSnapshot authorization = authorizationResolver.resolve(user.getId(), instanceId);
        AuthorizedSessionIssueParam authorizedParam = AuthorizedSessionIssueParam.builder()
                .userId(user.getId().toString())
                .instanceId(instanceId.toString())
                .roles(authorization.getRoles())
                .authVersion(user.getAuthVersion())
                .adminSessionVersion(authorization.getAdminSessionVersion())
                .ipAddress(param.getIpAddress())
                .userAgent(param.getUserAgent())
                .build();

        return issueAuthorizedSession(authorizedParam);
    }

    public AuthenticatedSessionReceipt issueAuthorizedSession(AuthorizedSessionIssueParam param) {
        Assert.notNull(param, "authorized session issue param is required");

        AuthorizedSessionIssueParam canonicalParam = AuthorizedSessionIssueParam.builder()
                .userId(param.getUserId())
                .instanceId(param.getInstanceId())
                .roles(canonicalRoles(param.getRoles(), param.getAdminSessionVersion()))
                .authVersion(param.getAuthVersion())
                .adminSessionVersion(param.getAdminSessionVersion())
                .ipAddress(param.getIpAddress())
                .userAgent(param.getUserAgent())
                .build();
        RefreshSessionReceipt refresh;

        try {
            refresh = refreshSessionStore.create(canonicalParam);
        } catch (RefreshSessionException exception) {
            throw mapFailure(exception);
        }

        try {
            return completeSession(refresh);
        } catch (RuntimeException signingFailure) {
            try {
                refreshSessionStore.revokeSession(refresh.getSessionId());
            } catch (RuntimeException cleanupFailure) {
                signingFailure.addSuppressed(cleanupFailure);
            }

            throw signingFailure;
        }
    }

    public AuthenticatedSessionReceipt completeSession(RefreshSessionReceipt refresh) {
        Assert.notNull(refresh, "refresh session receipt is required");

        RefreshSessionRecord record = refresh.getRecord();
        IssuedAccessTokenReceipt access = accessTokenIssuer.issueAccessToken(
                AccessTokenIssueParam.builder()
                        .userId(record.getUserId())
                        .instanceId(record.getInstanceId())
                        .sessionId(refresh.getSessionId())
                        .roles(record.getRoles())
                        .userAuthVersion(record.getAuthVersion())
                        .adminSessionVersion(record.getAdminSessionVersion())
                        .build());
        IssuedRefreshTokenReceipt refreshReceipt = IssuedRefreshTokenReceipt.builder()
                .sessionId(refresh.getSessionId())
                .token(refresh.getRefreshToken())
                .issuedAt(refresh.getIssuedAt())
                .expiresAt(record.getIdleExpiresAt())
                .build();

        return AuthenticatedSessionReceipt.builder()
                .accessToken(access)
                .refreshToken(refreshReceipt)
                .build();
    }

    private static RuntimeException mapFailure(RefreshSessionException exception) {
        return RemoteServiceException.unavailable("session-state", exception);
    }

    private static List<String> canonicalRoles(List<String> values, Long adminSessionVersion) {
        Assert.notEmpty(values, "roles are required");

        List<String> roles = new ArrayList<>(values);
        Collections.sort(roles);
        for (int i = 1; i < roles.size(); i++) {
            Assert.isTrue(!roles.get(i).equals(roles.get(i - 1)), "roles must be unique");
        }

        boolean administrator = roles.contains(INSTANCE_ADMIN_ROLE);
        Assert.isTrue(administrator == (adminSessionVersion != null),
                "INSTANCE_ADMIN role and adminSessionVersion must agree");
        return List.copyOf(roles);
    }

    private static void validatePrincipal(User user, UUID instanceId) {
        Assert.notNull(user, "user is required");
        Assert.notNull(user.getId(), "user id is required");
        Assert.notNull(instanceId, "instanceId is required");
        Assert.notNull(user.getAuthVersion(), "user authVersion is required");
        Assert.hasText(user.getEmail(), "user email is required");
    }

}
