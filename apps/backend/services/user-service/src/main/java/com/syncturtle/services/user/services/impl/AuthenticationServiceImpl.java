package com.syncturtle.services.user.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationResponse;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.common.spring.web.url.PublicUrlBuilder;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.user.client.InstanceClient;
import com.syncturtle.services.user.dto.command.CreateRefreshSessionCommand;
import com.syncturtle.services.user.dto.command.PassportTokenCommand;
import com.syncturtle.services.user.dto.command.RotateRefreshSessionCommand;
import com.syncturtle.services.user.dto.internal.RefreshExchangeResult;
import com.syncturtle.services.user.dto.internal.UserAuthRuntimeConfig;
import com.syncturtle.services.user.dto.request.SignInRequest;
import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.models.Instance;
import com.syncturtle.services.user.models.User;
import com.syncturtle.services.user.payload.IssuedPassport;
import com.syncturtle.services.user.payload.IssuedRefreshToken;
import com.syncturtle.services.user.repositories.InstanceRepository;
import com.syncturtle.services.user.repositories.UserRepository;
import com.syncturtle.services.user.repositories.projections.UserPasswordAutosetProjection;
import com.syncturtle.services.user.services.AuthenticationService;
import com.syncturtle.services.user.services.FeatureFlagService;
import com.syncturtle.services.user.services.PassportService;
import com.syncturtle.services.user.services.RefreshTokenService;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String INSTANCE_ADMIN_ROLE = "INSTANCE_ADMIN";

    // context
    private final RequestUserContext userContext;
    private final RequestClientContext clientContext;
    // host resolver
    private final PublicUrlBuilder hostResolver;
    // repositories
    private final InstanceRepository instanceRepository;
    private final UserRepository userRepository;
    // services
    private final FeatureFlagService featureFlagService;
    private final PassportService passportService;
    private final RefreshTokenService refreshTokenService;
    // utilities
    private final StringRedisTemplate redis;
    private final PasswordEncoder passwordEncoder;
    private final InstanceClient instanceClient;

    @Override
    @Transactional(readOnly = true)
    public EmailCheckResponse emailCheck(String rawEmail) {
        requireSetupInstance();

        UserAuthRuntimeConfig configurations = featureFlagService.getInstanceConfigurations();

        boolean smtpConfigured = configurations.isSmtpEnabled();
        boolean isMagicLoginEnabled = configurations.isMagicLinkEnabled();

        String email = rawEmail.trim().toLowerCase();

        Optional<UserPasswordAutosetProjection> user = userRepository.findByEmailIgnoreCase(email,
                UserPasswordAutosetProjection.class);
        if (user.isPresent()) {
            boolean shouldMagic = user.get().isPasswordAutoset() && smtpConfigured && isMagicLoginEnabled;
            return new EmailCheckResponse(shouldMagic ? "MAGIC_CODE" : "CREDENTIAL", true, false);
        }
        boolean shouldMagic = smtpConfigured && isMagicLoginEnabled;
        return new EmailCheckResponse(shouldMagic ? "MAGIC_CODE" : "CREDENTIAL", false, false);
    }

    @Override
    @Transactional
    public IssueTokenResponse signin(SignInRequest request) {
        Instance instance = requireSetupInstance();

        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();

        User user = userRepository.findByEmailIgnoreCase(email, User.class).orElse(null);

        if (user == null || !user.isActive()) {
            throw AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED)
                    .with("message", "Invalid credentails");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        Instant now = Instant.now();

        user.setLastActive(now);
        user.setLastLoginTime(now);
        user.setLastLoginIp(clientContext.getClientIp());
        user.setLastLoginUagent(clientContext.getUserAgent());
        user.setTokenUpdatedAt(now);

        User updated = userRepository.saveAndFlush(user);

        AuthzSnapshot authz = resolveCurrentAuthz(updated.getId(), instance.getId());

        return issueSession(updated, instance, authz, clientContext.getClientIp(), clientContext.getUserAgent());
    }

    @Override
    @Transactional
    public RefreshExchangeResult refreshSession(String presentedRefreshToken) {
        if (!StringUtils.hasText(presentedRefreshToken)) {
            throw invalidRefresh("Missing refresh token");
        }

        String sessionId = extractSessionId(presentedRefreshToken);

        RefreshSessionRecord session = refreshTokenService.findSession(sessionId)
                .orElseThrow(() -> invalidRefresh("Refresh session not found"));

        validateRefreshSessionState(sessionId, session, presentedRefreshToken);

        User user = userRepository.findById(UUID.fromString(session.getUserId())).orElse(null);

        if (user == null || !user.isActive()) {
            refreshTokenService.revokeSession(sessionId);
            throw invalidRefresh("User is missing or inactive");
        }

        if (!Objects.equals(user.getAuthVersion(), session.getAuthVersion())) {
            refreshTokenService.revokeSession(sessionId);
            throw invalidRefresh("Refresh session is stale");
        }

        Instance instance = instanceRepository.findById(UUID.fromString(session.getInstanceId())).orElse(null);

        if (instance == null || !instance.isSetupDone()) {
            refreshTokenService.revokeSession(sessionId);
            throw invalidRefresh("Instance is not available");
        }

        AuthzSnapshot currentAuthz = resolveCurrentAuthz(user.getId(), instance.getId());

        validateAdminStateForRefresh(sessionId, session, currentAuthz);

        IssuedRefreshToken rotatedRefresh = refreshTokenService.rotateRefreshToken(
                RotateRefreshSessionCommand.builder()
                        .sessionId(sessionId)
                        .userId(session.getUserId())
                        .instanceId(session.getInstanceId())
                        .email(user.getEmail())
                        .roles(currentAuthz.getRoles())
                        .authVersion(user.getAuthVersion())
                        .adminSessionVersion(currentAuthz.getAdminSessionVersion())
                        .ipAddress(clientContext.getClientIp())
                        .userAgent(clientContext.getUserAgent())
                        .build());

        IssuedPassport access = passportService.issueAccessToken(
                PassportTokenCommand.builder()
                        .userId(session.getUserId())
                        .instanceId(session.getInstanceId())
                        .sessionId(rotatedRefresh.getSessionId())
                        .roles(currentAuthz.getRoles())
                        .userAuthVersion(user.getAuthVersion())
                        .adminSessionVersion(currentAuthz.getAdminSessionVersion())
                        .build());

        writeCurrentVersionKeys(
                session.getUserId(),
                session.getInstanceId(),
                user.getAuthVersion(),
                currentAuthz.getAdminSessionVersion());

        return new RefreshExchangeResult(access, rotatedRefresh);
    }

    @Override
    @Transactional
    public String signOut(String logoutContext, String sessionId) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(logoutContext);

        String redirectionUrl = isAdmin
                ? hostResolver.admin("")
                : hostResolver.userApp("/sign-in");

        if (StringUtils.hasText(sessionId)) {
            refreshTokenService.revokeSession(sessionId);
        }
        User user = userRepository.findById(userContext.getUserId()).orElse(null);

        if (user == null) {
            return redirectionUrl;
        }

        try {
            user.setLastLogoutIp(clientContext.getClientIp());
            user.setLastLogoutTime(Instant.now());
            userRepository.save(user);
        } catch (Exception exception) {
            log.warn("Failed to update logout audit (continuing): {}", exception.toString());
        }

        return redirectionUrl;
    }

    private IssueTokenResponse issueSession(
            User user,
            Instance instance,
            AuthzSnapshot authz,
            String ipAddress,
            String userAgent) {
        IssuedRefreshToken refresh = refreshTokenService.createSessionRefreshToken(
                CreateRefreshSessionCommand.builder()
                        .userId(user.getId().toString())
                        .instanceId(instance.getId().toString())
                        .email(user.getEmail())
                        .roles(authz.getRoles())
                        .authVersion(user.getAuthVersion())
                        .adminSessionVersion(authz.getAdminSessionVersion())
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .build());

        IssuedPassport access = passportService.issueAccessToken(
                PassportTokenCommand.builder()
                        .userId(user.getId().toString())
                        .instanceId(instance.getId().toString())
                        .sessionId(refresh.getSessionId())
                        .roles(authz.getRoles())
                        .userAuthVersion(user.getAuthVersion())
                        .adminSessionVersion(authz.getAdminSessionVersion())
                        .build());

        writeCurrentVersionKeys(
                user.getId().toString(),
                instance.getId().toString(),
                user.getAuthVersion(),
                authz.getAdminSessionVersion());

        return IssueTokenResponse.builder()
                .accessToken(access.getToken())
                .refreshToken(refresh.getToken())
                .accessIssuedAt(access.getIssuedAt())
                .accessExpiresAt(access.getExpiresAt())
                .refreshIssuedAt(refresh.getIssuedAt())
                .refreshExpiresAt(refresh.getExpiresAt())
                .build();
    }

    private AuthzSnapshot resolveCurrentAuthz(UUID userId, UUID instanceId) {
        ResolveInstanceAuthorizationResponse response = instanceClient.resolveAuthz(
                new ResolveInstanceAuthorizationRequest(userId, instanceId));

        if (response.isInstanceAdmin()) {
            return AuthzSnapshot.builder()
                    .roles(List.of(INSTANCE_ADMIN_ROLE))
                    .adminSessionVersion(response.getAdminSessionVersion())
                    .build();
        }

        return AuthzSnapshot.builder()
                .roles(List.of())
                .adminSessionVersion(null)
                .build();
    }

    private void validateRefreshSessionState(
            String sessionId,
            RefreshSessionRecord session,
            String presentedRefreshToken) {
        if (!session.isActive()) {
            refreshTokenService.revokeSession(sessionId);
            throw invalidRefresh("Refresh session inactive");
        }

        if (session.getExpiresAt() == null || !session.getExpiresAt().isAfter(Instant.now())) {
            refreshTokenService.revokeSession(sessionId);
            throw invalidRefresh("Refresh session expired");
        }

        if (!refreshTokenService.matchesStoredRefreshToken(presentedRefreshToken, session)) {
            refreshTokenService.revokeSession(sessionId);
            throw invalidRefresh("Refresh token mismatch");
        }
    }

    private void validateAdminStateForRefresh(
            String sessionId,
            RefreshSessionRecord session,
            AuthzSnapshot currentAuthz) {
        boolean sessionWasAdmin = session.getAdminSessionVersion() != null;

        if (!sessionWasAdmin) {
            return;
        }

        if (!currentAuthz.isInstanceAdmin()) {
            refreshTokenService.revokeSession(sessionId);
        }

        if (!Objects.equals(session.getAdminSessionVersion(), currentAuthz.getAdminSessionVersion())) {
            refreshTokenService.revokeSession(sessionId);
            throw invalidRefresh("Admin session version is stale");
        }
    }

    private void writeCurrentVersionKeys(
            String userId,
            String instanceId,
            Long userAuthVersion,
            Long adminSessionVersion) {
        redis.opsForValue().set(
                refreshTokenService.currentUserAuthVersionKey(userId),
                Long.toString(userAuthVersion));

        if (adminSessionVersion != null) {
            redis.opsForValue().set(
                    refreshTokenService.currentAdminSessionVersionKey(instanceId, userId),
                    Long.toString(adminSessionVersion));
        }
    }

    private String extractSessionId(String presentedRefreshToken) {
        try {
            return refreshTokenService.extractSessionIdFromRefreshToken(presentedRefreshToken);
        } catch (IllegalArgumentException exception) {
            throw invalidRefresh("Invalid refresh token format");
        }
    }

    private Instance requireSetupInstance() {
        Instance instance = instanceRepository.findFirstByOrderByCreatedAtAsc().orElse(null);

        if (instance == null || !instance.isSetupDone()) {
            throw new AuthException(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
        }

        return instance;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private AuthException invalidRefresh(String message) {
        return AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED)
                .with("message", message);
    }

    @Getter
    @Builder
    private static class AuthzSnapshot {
        private List<String> roles;
        private Long adminSessionVersion;

        private boolean isInstanceAdmin() {
            return roles != null && roles.contains(INSTANCE_ADMIN_ROLE);
        }
    }

}
