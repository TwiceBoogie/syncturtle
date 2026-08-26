package com.syncturtle.services.user.service.collaborator.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.exception.RefreshSessionLifecycleException;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.token.AccessTokenIssuer;
import com.syncturtle.services.user.service.collaborator.token.IssuedAccessTokenReceipt;
import com.syncturtle.services.user.service.param.AccessTokenIssueParam;
import com.syncturtle.services.user.service.param.AuthenticatedSessionIssueParam;
import com.syncturtle.services.user.service.param.AuthorizedSessionIssueParam;
import com.syncturtle.services.user.service.param.RefreshSessionFamilyCreateParam;
import com.syncturtle.services.user.service.param.RefreshSessionFamilyRotateParam;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticatedSessionIssuer {

    private static final String INSTANCE_ADMIN_ROLE = "INSTANCE_ADMIN";

    private final RefreshSessionFamilyStore refreshSessionFamilyStore;
    private final RefreshSessionClientFingerprintFactory fingerprintFactory;
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

        List<String> roles = canonicalRoles(param.getRoles(), param.getAdminSessionVersion());
        RefreshSessionClientFingerprint fingerprint = fingerprintFactory.create(param.getIpAddress(),
                param.getUserAgent());
        RefreshSessionLifecycleResult refresh;
        try {
            refresh = refreshSessionFamilyStore.create(
                    RefreshSessionFamilyCreateParam.builder()
                            .userId(param.getUserId())
                            .instanceId(param.getInstanceId())
                            .roles(roles)
                            .authVersion(param.getAuthVersion())
                            .adminSessionVersion(param.getAdminSessionVersion())
                            .deviceLabel(fingerprint.getDeviceLabel())
                            .clientBindingHash(fingerprint.getClientBindingHash())
                            .build());
        } catch (RefreshSessionLifecycleException exception) {
            throw mapInfrastructureFailure(exception);
        }

        try {
            return completeSession(refresh);
        } catch (RuntimeException signingFailure) {
            revokeFailedCreation(refresh, signingFailure);
            throw signingFailure;
        }
    }

    public AuthenticatedSessionReceipt rotateSession(
            AuthenticatedSessionIssueParam param,
            String presentedRefreshToken,
            InstanceAuthorizationSnapshot authorization) {
        Assert.notNull(param, "param is required");
        Assert.hasText(presentedRefreshToken, "presentedRefreshToken is required");
        Assert.notNull(authorization, "authorization is required");

        User user = param.getUser();
        validatePrincipal(user, param.getInstanceId());

        List<String> roles = canonicalRoles(
                authorization.getRoles(),
                authorization.getAdminSessionVersion());
        RefreshSessionClientFingerprint fingerprint = fingerprintFactory.create(
                param.getIpAddress(),
                param.getUserAgent());
        RefreshSessionLifecycleResult refresh;
        try {
            refresh = refreshSessionFamilyStore.rotate(
                    RefreshSessionFamilyRotateParam.builder()
                            .presentedRefreshToken(presentedRefreshToken)
                            .roles(roles)
                            .authVersion(user.getAuthVersion())
                            .adminSessionVersion(authorization.getAdminSessionVersion())
                            .deviceLabel(fingerprint.getDeviceLabel())
                            .clientBindingHash(fingerprint.getClientBindingHash())
                            .build());
        } catch (RefreshSessionLifecycleException exception) {
            throw mapInfrastructureFailure(exception);
        }

        // If access signing fails after rotation, the browser still has the previous
        // refresh token. The one bounded grace recovery is its safe retry path.
        return completeSession(refresh);
    }

    private AuthenticatedSessionReceipt completeSession(RefreshSessionLifecycleResult refresh) {
        RefreshSessionFamilyRecord family = refresh.getFamily();
        IssuedAccessTokenReceipt access = accessTokenIssuer.issueAccessToken(
                AccessTokenIssueParam.builder()
                        .userId(family.getUserId())
                        .instanceId(family.getInstanceId())
                        .sessionId(refresh.getSessionId())
                        .roles(family.getRoles())
                        .userAuthVersion(family.getAuthVersion())
                        .adminSessionVersion(family.getAdminSessionVersion())
                        .build());
        IssuedRefreshTokenReceipt refreshReceipt = IssuedRefreshTokenReceipt.builder()
                .sessionId(refresh.getSessionId())
                .token(refresh.getRefreshToken())
                .issuedAt(family.getLastUsedAt())
                .expiresAt(family.getIdleExpiresAt())
                .build();

        return AuthenticatedSessionReceipt.builder()
                .accessToken(access)
                .refreshToken(refreshReceipt)
                .build();
    }

    private void revokeFailedCreation(RefreshSessionLifecycleResult refresh, RuntimeException signingFailure) {
        try {
            refreshSessionFamilyStore.revokeOne(
                    refresh.getFamily().getUserId(),
                    refresh.getSessionId());
        } catch (RuntimeException cleanupFailure) {
            signingFailure.addSuppressed(cleanupFailure);
        }
    }

    private static RuntimeException mapInfrastructureFailure(RefreshSessionLifecycleException exception) {
        if (exception.getReason() == RefreshSessionLifecycleException.Reason.REDIS_FAILURE
                || exception.getReason() == RefreshSessionLifecycleException.Reason.INVARIANT_VIOLATION) {
            return RemoteServiceException.unavailable("session-state", exception);
        }
        return exception;
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
