package com.syncturtle.services.user.service.collaborator.session;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.token.AccessTokenIssuer;
import com.syncturtle.services.user.service.collaborator.token.IssuedAccessTokenReceipt;
import com.syncturtle.services.user.service.param.AccessTokenIssueParam;
import com.syncturtle.services.user.service.param.AuthenticatedSessionIssueParam;
import com.syncturtle.services.user.service.param.RefreshSessionCreateParam;
import com.syncturtle.services.user.service.param.RefreshSessionRotateParam;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticatedSessionIssuer {

    private final RefreshSessionTokenStore refreshSessionTokenStore;
    private final AccessTokenIssuer accessTokenIssuer;
    private final InstanceAuthorizationResolver authorizationResolver;
    private final SessionVersionWriter sessionVersionWriter;

    public AuthenticatedSessionReceipt issueNewSession(AuthenticatedSessionIssueParam spec) {
        Assert.notNull(spec, "spec is required");

        User user = spec.getUser();
        UUID instance = spec.getInstanceId();

        validatePrincipal(user, instance);

        InstanceAuthorizationSnapshot authz = authorizationResolver.resolve(
                user.getId(),
                instance);

        RefreshSessionCreateParam refreshSpec = RefreshSessionCreateParam.builder()
                .userId(user.getId().toString())
                .instanceId(instance.toString())
                .email(user.getEmail())
                .roles(authz.getRoles())
                .authVersion(user.getAuthVersion())
                .adminSessionVersion(authz.getAdminSessionVersion())
                .ipAddress(spec.getIpAddress())
                .userAgent(spec.getUserAgent())
                .build();
        IssuedRefreshTokenReceipt refresh = refreshSessionTokenStore.createSessionRefreshToken(refreshSpec);
        IssuedAccessTokenReceipt access = issueAccessToken(user, instance, refresh.getSessionId(), authz);

        writeVersionKeys(user, instance, authz);

        return AuthenticatedSessionReceipt.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .build();
    }

    public AuthenticatedSessionReceipt rotateSession(AuthenticatedSessionIssueParam spec, String sessionId) {
        Assert.notNull(spec, "spec is required");
        Assert.notNull(sessionId, "sessionId is required");

        User user = spec.getUser();
        UUID instanceId = spec.getInstanceId();
        InstanceAuthorizationSnapshot authz = authorizationResolver.resolve(user.getId(), instanceId);

        validatePrincipal(user, instanceId);

        RefreshSessionRotateParam refreshSpec = RefreshSessionRotateParam.builder()
                .sessionId(sessionId)
                .userId(user.getId().toString())
                .instanceId(instanceId.toString())
                .email(user.getEmail())
                .roles(authz.getRoles())
                .authVersion(user.getAuthVersion())
                .adminSessionVersion(authz.getAdminSessionVersion())
                .ipAddress(spec.getIpAddress())
                .userAgent(spec.getUserAgent())
                .build();
        IssuedRefreshTokenReceipt refresh = refreshSessionTokenStore.rotateRefreshToken(refreshSpec);
        IssuedAccessTokenReceipt access = issueAccessToken(user, instanceId, sessionId, authz);

        writeVersionKeys(user, instanceId, authz);

        return AuthenticatedSessionReceipt.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .build();
    }

    private IssuedAccessTokenReceipt issueAccessToken(
            User user,
            UUID instanceId,
            String sessionId,
            InstanceAuthorizationSnapshot authz) {
        Assert.notNull(user, "user is required");
        Assert.notNull(instanceId, "instanceId is required");
        Assert.hasText(sessionId, "sessionId is required");
        Assert.notNull(authz, "authorization is required");

        AccessTokenIssueParam accessSpec = AccessTokenIssueParam.builder()
                .userId(user.getId().toString())
                .instanceId(instanceId.toString())
                .sessionId(sessionId)
                .roles(authz.getRoles())
                .userAuthVersion(user.getAuthVersion())
                .adminSessionVersion(authz.getAdminSessionVersion())
                .build();
        return accessTokenIssuer.issueAccessToken(accessSpec);
    }

    private void writeVersionKeys(
            User user,
            UUID instanceId,
            InstanceAuthorizationSnapshot authz) {
        sessionVersionWriter.write(
                user.getId().toString(),
                instanceId.toString(),
                user.getAuthVersion(),
                authz.getAdminSessionVersion());
    }

    private void validatePrincipal(User user, UUID instanceId) {
        Assert.notNull(user.getId(), "user id is required");
        Assert.notNull(instanceId, "instanceId id is required");
        Assert.notNull(user.getId(), "user id is required");
        Assert.notNull(user.getAuthVersion(), "user authVersion is required");
        Assert.hasText(user.getEmail(), "user email is required");
    }

}
