package com.syncturtle.services.user.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import com.syncturtle.common.contracts.instance.admin.AdminSigninResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSignupResponse;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.IssueInstanceAdminSessionRequest;
import com.syncturtle.common.contracts.auth.session.IssueSessionResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;
import com.syncturtle.services.user.dto.command.CreateRefreshSessionCommand;
import com.syncturtle.services.user.dto.command.PassportTokenCommand;
import com.syncturtle.services.user.models.Profile;
import com.syncturtle.services.user.models.User;
import com.syncturtle.services.user.payload.IssuedPassport;
import com.syncturtle.services.user.payload.IssuedRefreshToken;
import com.syncturtle.services.user.payload.UserEventToPublish;
import com.syncturtle.services.user.repositories.ProfileRepository;
import com.syncturtle.services.user.repositories.UserRepository;
import com.syncturtle.services.user.services.PassportService;
import com.syncturtle.services.user.services.RefreshTokenService;
import com.syncturtle.services.user.services.UserAdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private static final String INSTANCE_ADMIN_ROLE = "INSTANCE_ADMIN";

    // services
    private final PassportService passportService;
    private final RefreshTokenService refreshTokenService;
    // repositories
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    // helpers
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher events;
    private final PasswordEncoder passwordEncoder;
    private final Zxcvbn zxcvbn = new Zxcvbn();

    @Override
    @Transactional
    public AdminSignupResponse adminSignup(AdminSignupRequest request) {
        validateSignupRequest(request);

        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        String email = normalizeEmail(request.getEmail());
        String companyName = request.getCompanyName();
        boolean telemetryEnabled = request.isTelemetryEnabled();
        String password = request.getPassword();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw AuthException.of(AuthErrorCode.ADMIN_USER_ALREADY_EXIST)
                    .with("email", email)
                    .with("firstName", firstName)
                    .with("lastName", lastName)
                    .with("companyName", companyName)
                    .with("isTelemetryEnabled", telemetryEnabled);
        }

        Strength result = zxcvbn.measure(password);
        if (result.getScore() < 3) {
            throw AuthException.of(AuthErrorCode.INVALID_ADMIN_PASSWORD)
                    .with("email", email)
                    .with("firstName", firstName)
                    .with("lastName", lastName)
                    .with("companyName", companyName)
                    .with("isTelemetryEnabled", telemetryEnabled);
        }

        Instant now = Instant.now();

        User user = User.create(
                email,
                firstName,
                lastName,
                passwordEncoder.encode(password),
                UUID.randomUUID().toString().replace("-", ""),
                false,
                request.getClientIp(),
                request.getUserAgent());

        User saved = userRepository.saveAndFlush(user);
        profileRepository.save(Profile.create(saved, companyName));

        publishUserEvent(user, UserEvent.Type.USER_CREATED, now);

        return new AdminSignupResponse(saved.getId(), saved.getAuthVersion());
    }

    @Override
    @Transactional
    public AdminSigninResponse adminSignin(AdminSigninRequest request) {
        validateSigninRequest(request);

        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();

        User user = userRepository.findByEmailIgnoreCase(email, User.class).orElse(null);

        if (user == null) {
            throw AuthException.of(AuthErrorCode.ADMIN_USER_DOES_NOT_EXIST)
                    .with("email", email);
        }

        if (!user.isActive()) {
            throw AuthException.of(AuthErrorCode.ADMIN_USER_DEACTIVATED);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw AuthException.of(AuthErrorCode.ADMIN_AUTHENTICATION_FAILED);
        }

        Instant now = Instant.now();
        user.setActive(true);
        user.setLastActive(now);
        user.setLastLoginTime(now);
        user.setLastLoginIp(request.getClientIp());
        user.setLastLoginUagent(request.getUserAgent());
        user.setTokenUpdatedAt(now);
        // IMPORTANT: we don't bump authVersion on normal successful signin
        // to guarantee the @Version field is updated before publishing the event
        User updated = userRepository.saveAndFlush(user);

        publishUserEvent(updated, UserEvent.Type.USER_UPDATED, now);

        return new AdminSigninResponse(updated.getId(), updated.getAuthVersion());
    }

    @Override
    @Transactional
    public IssueSessionResponse issueInstanceAdminSession(
            IssueInstanceAdminSessionRequest request) {
        validateIssueInstanceAdminSessionRequest(request);

        User user = userRepository.findById(request.getUserId()).orElse(null);

        if (user == null) {
            throw AuthException.of(AuthErrorCode.ADMIN_USER_DOES_NOT_EXIST);
        }

        if (!user.isActive()) {
            throw AuthException.of(AuthErrorCode.ADMIN_USER_DEACTIVATED);
        }

        if (!Objects.equals(user.getAuthVersion(), request.getUserAuthVersion())) {
            throw AuthException.of(AuthErrorCode.ADMIN_AUTHENTICATION_FAILED);
        }

        List<String> roles = List.of(INSTANCE_ADMIN_ROLE);

        IssuedRefreshToken refresh = refreshTokenService.createSessionRefreshToken(
                CreateRefreshSessionCommand.builder()
                        .userId(request.getUserId().toString())
                        .instanceId(request.getInstanceId().toString())
                        .email(user.getEmail())
                        .roles(roles)
                        .authVersion(request.getUserAuthVersion())
                        .adminSessionVersion(request.getAdminSessionVersion())
                        .ipAddress(request.getClientIp())
                        .userAgent(request.getUserAgent())
                        .build());

        IssuedPassport access = passportService.issueAccessToken(
                PassportTokenCommand.builder()
                        .userId(request.getUserId().toString())
                        .instanceId(request.getInstanceId().toString())
                        .sessionId(refresh.getSessionId())
                        .roles(roles)
                        .userAuthVersion(request.getUserAuthVersion())
                        .adminSessionVersion(request.getAdminSessionVersion())
                        .build());

        redis.opsForValue().set(
                refreshTokenService.currentUserAuthVersionKey(request.getUserId().toString()),
                Long.toString(user.getAuthVersion()));

        redis.opsForValue().set(
                refreshTokenService.currentAdminSessionVersionKey(
                        request.getInstanceId().toString(),
                        request.getUserId().toString()),
                Long.toString(request.getAdminSessionVersion()));

        return IssueSessionResponse.builder()
                .accessToken(access.getToken())
                .refreshToken(refresh.getToken())
                .accessIssuedAt(access.getIssuedAt())
                .accessExpiresAt(access.getExpiresAt())
                .refreshIssuedAt(refresh.getIssuedAt())
                .refreshExpiresAt(refresh.getExpiresAt())
                .build();
    }

    private void validateSignupRequest(AdminSignupRequest request) {
        Assert.notNull(request, "request is required");
        Assert.hasText(request.getFirstName(), "firstName is required");
        Assert.hasText(request.getLastName(), "lastName is required");
        Assert.hasText(request.getEmail(), "email is required");
        Assert.hasText(request.getPassword(), "password is required");
        Assert.hasText(request.getCompanyName(), "companyName is required");
    }

    private void validateSigninRequest(AdminSigninRequest request) {
        Assert.notNull(request, "request is required");
        Assert.hasText(request.getEmail(), "email is required");
        Assert.hasText(request.getPassword(), "password is required");
    }

    private void validateIssueInstanceAdminSessionRequest(IssueInstanceAdminSessionRequest request) {
        Assert.notNull(request, "request is required");
        Assert.notNull(request.getUserId(), "userId is required");
        Assert.notNull(request.getInstanceId(), "instanceId is required");
        Assert.notNull(request.getUserAuthVersion(), "userAuthVersion is required");
        Assert.notNull(request.getAdminSessionVersion(), "adminSessionVersion is required");
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        return email.trim().toLowerCase();
    }

    private void publishUserEvent(User user, UserEvent.Type type, Instant now) {
        UserEvent evt = UserEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(now)
                .type(type)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateJoined(user.getCreatedAt())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .passwordAutoset(user.isPasswordAutoset())
                .userTimezone(user.getUserTimezone())
                .bot(user.isBot())
                .version(user.getVersion())
                .authVersion(user.getAuthVersion())
                .build();

        events.publishEvent(new UserEventToPublish(evt));
    }

}
