package com.syncturtle.services.user.service.impl;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
import com.syncturtle.common.core.actor.PrincipalType;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.IssueInstanceAdminSessionRequest;
import com.syncturtle.common.contracts.auth.session.IssueSessionResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.messaging.outbox.UserOutboxWriter;
import com.syncturtle.services.user.model.Profile;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.model.param.UserCreateParam;
import com.syncturtle.services.user.repository.ProfileRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.UserAdminService;
import com.syncturtle.services.user.service.session.IssuedRefreshTokenReceipt;
import com.syncturtle.services.user.service.session.RefreshSessionCreateSpec;
import com.syncturtle.services.user.service.session.RefreshSessionTokenStore;
import com.syncturtle.services.user.service.session.SessionVersionWriter;
import com.syncturtle.services.user.service.token.AccessTokenIssueSpec;
import com.syncturtle.services.user.service.token.AccessTokenIssuer;
import com.syncturtle.services.user.service.token.IssuedAccessTokenReceipt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private static final String INSTANCE_ADMIN_ROLE = "INSTANCE_ADMIN";

    private final AccessTokenIssuer accessTokenIssuer;
    private final SessionVersionWriter sessionVersionWriter;
    private final RefreshSessionTokenStore refreshSessionTokenStore;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserEventFactory userEventFactory;
    private final UserOutboxWriter outboxWriter;
    private final PasswordEncoder passwordEncoder;
    private final Zxcvbn zxcvbn = new Zxcvbn();
    private final Clock clock;

    @Override
    @Transactional
    public AdminSignupResponse adminSignup(AdminSignupRequest request) {
        Assert.notNull(request, "request is required");

        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        String email = normalizeEmail(request.getEmail());
        String companyName = request.getCompanyName();
        boolean telemetryEnabled = request.isTelemetryEnabled();
        String password = request.getPassword();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw AuthException.of(AuthErrorCode.ADMIN_USER_ALREADY_EXISTS)
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

        UserCreateParam param = UserCreateParam.builder()
                .username(UUID.randomUUID().toString().replace("-", ""))
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .passwordHash(passwordEncoder.encode(password))
                .passwordAutoset(false)
                .principalType(PrincipalType.HUMAN)
                .initialLoginIp(request.getClientIp())
                .initialLoginMedium("PASSWORD")
                .initialLoginUserAgent(request.getUserAgent())
                .build();
        User user = User.create(param, clock);

        User saved = userRepository.saveAndFlush(user);
        profileRepository.save(Profile.create(saved, clock, companyName));

        UserEvent event = userEventFactory.created(saved);
        outboxWriter.saveUserEvent(event);

        return new AdminSignupResponse(saved.getId(), saved.getAuthVersion());
    }

    @Override
    @Transactional
    public AdminSigninResponse adminSignin(AdminSigninRequest request) {
        Assert.notNull(request, "request is required");

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

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw AuthException.of(AuthErrorCode.ADMIN_AUTHENTICATION_FAILED);
        }

        user.recordSuccessfullLogin("password", request.getClientIp(), request.getUserAgent(), clock);
        // IMPORTANT: we don't bump authVersion on normal successful signin
        // to guarantee the @Version field is updated before publishing the event
        User updated = userRepository.saveAndFlush(user);

        // NOTE: no need to publish since no field thats wanted is updated

        return new AdminSigninResponse(updated.getId(), updated.getAuthVersion());
    }

    @Override
    @Transactional
    public IssueSessionResponse issueInstanceAdminSession(
            IssueInstanceAdminSessionRequest request) {
        Assert.notNull(request, "request is required");

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

        IssuedRefreshTokenReceipt refresh = refreshSessionTokenStore.createSessionRefreshToken(
                RefreshSessionCreateSpec.builder()
                        .userId(request.getUserId().toString())
                        .instanceId(request.getInstanceId().toString())
                        .email(user.getEmail())
                        .roles(roles)
                        .authVersion(request.getUserAuthVersion())
                        .adminSessionVersion(request.getAdminSessionVersion())
                        .ipAddress(request.getClientIp())
                        .userAgent(request.getUserAgent())
                        .build());

        IssuedAccessTokenReceipt access = accessTokenIssuer.issueAccessToken(
                AccessTokenIssueSpec.builder()
                        .sessionId(refresh.getSessionId())
                        .userId(request.getUserId().toString())
                        .instanceId(request.getInstanceId().toString())
                        .roles(roles)
                        .userAuthVersion(request.getUserAuthVersion())
                        .adminSessionVersion(request.getAdminSessionVersion())
                        .build());

        sessionVersionWriter.write(user.getId().toString(), request.getInstanceId().toString(),
                request.getUserAuthVersion(), request.getAdminSessionVersion());

        return IssueSessionResponse.builder()
                .accessToken(access.getToken())
                .refreshToken(refresh.getToken())
                .accessIssuedAt(access.getIssuedAt())
                .accessExpiresAt(access.getExpiresAt())
                .refreshIssuedAt(refresh.getIssuedAt())
                .refreshExpiresAt(refresh.getExpiresAt())
                .build();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        return email.trim().toLowerCase();
    }

}
