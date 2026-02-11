package com.syncturtle.platform.services.user.services.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.events.UserEvent;
import com.syncturtle.common.core.events.UserEvent.Type;
import com.syncturtle.common.core.exceptions.AuthenticationException;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.dto.request.AdminSigninInternalRequest;
import com.syncturtle.common.web.dto.request.AdminSignupInternalRequest;
import com.syncturtle.common.web.dto.response.AdminSigninInternalResponse;
import com.syncturtle.common.web.dto.response.AdminSignupInternalResponse;
import com.syncturtle.platform.services.user.models.Profile;
import com.syncturtle.platform.services.user.models.User;
import com.syncturtle.platform.services.user.payload.UserEventToPublish;
import com.syncturtle.platform.services.user.repositories.ProfileRepository;
import com.syncturtle.platform.services.user.repositories.UserRepository;
import com.syncturtle.platform.services.user.services.UserAdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

        // repositories
        private final UserRepository userRepository;
        private final ProfileRepository profileRepository;
        // helpers
        private final ApplicationEventPublisher events;
        private final PasswordEncoder passwordEncoder;
        private final Zxcvbn zxcvbn = new Zxcvbn();
        // context
        private final RequestClientContext ctx;

        @Override
        @Transactional
        public AdminSignupInternalResponse adminSignup(AdminSignupInternalRequest request) {
                String firstName = request.getFirstName();
                String lastName = request.getLastName();
                String email = request.getEmail().trim().toLowerCase();
                String companyName = request.getCompanyName();
                boolean telemetryEnabled = request.isTelemetryEnabled();
                String password = request.getPassword();

                if (userRepository.existsByEmailIgnoreCase(email)) {
                        // we can throw and have global exception handler catch it but might be hard to
                        // know which path to redirect/return json
                        throw AuthenticationException.of(AuthErrorCode.ADMIN_USER_ALREADY_EXIST)
                                        .with("email", email)
                                        .with("firstName", firstName)
                                        .with("lastName", lastName)
                                        .with("companyName", companyName)
                                        .with("isTelemetryEnabled", telemetryEnabled);
                }

                Strength result = zxcvbn.measure(password);
                if (result.getScore() < 3) {
                        throw AuthenticationException.of(AuthErrorCode.INVALID_ADMIN_PASSWORD)
                                        .with("email", email)
                                        .with("firstName", firstName)
                                        .with("lastName", lastName)
                                        .with("companyName", companyName)
                                        .with("isTelemetryEnabled", telemetryEnabled);
                }

                Instant now = Instant.now();
                log.info("user IP: {}", ctx.getClientIp());
                User user = userRepository.saveAndFlush(User.create(
                                email,
                                firstName,
                                lastName,
                                passwordEncoder.encode(password),
                                UUID.randomUUID().toString().replace("-", ""),
                                false,
                                ctx.getClientIp(),
                                ctx.getUserAgent()));

                profileRepository.save(Profile.create(user, companyName));

                // user.setActive(true);
                // user.setLastActive(now);
                // user.setLastLoginTime(now);
                // user.setLastLoginIp(ctx.getClientIp());
                // user.setLastLoginUagent(ctx.getUserAgent());
                // user.setTokenUpdatedAt(now);
                // to guarantee the @Version field is updated before publishing the event
                // User updated = userRepository.saveAndFlush(user);

                UserEvent evt = UserEvent.builder()
                                .eventId(UUID.randomUUID().toString())
                                .occurredAt(now)
                                .type(Type.USER_CREATED)
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
                                .build();

                events.publishEvent(new UserEventToPublish(evt));

                return new AdminSignupInternalResponse(user.getId());
        }

        @Override
        @Transactional
        public AdminSigninInternalResponse adminSignin(AdminSigninInternalRequest request) {
                String email = request.getEmail().trim().toLowerCase();
                String password = request.getPassword();

                User user = userRepository.findByEmailIgnoreCase(email, User.class).orElse(null);

                if (user == null) {
                        throw AuthenticationException.of(AuthErrorCode.ADMIN_USER_DOES_NOT_EXIST)
                                        .with("email", email);
                }

                if (!user.isActive()) {
                        throw AuthenticationException.of(AuthErrorCode.ADMIN_USER_DEACTIVATED);
                }

                if (!passwordEncoder.matches(password, user.getPassword())) {
                        throw AuthenticationException.of(AuthErrorCode.ADMIN_AUTHENTICATION_FAILED);
                }

                Instant now = Instant.now();
                user.setActive(true);
                user.setLastActive(now);
                user.setLastLoginTime(now);
                user.setLastLoginIp(ctx.getClientIp());
                user.setLastLoginUagent(ctx.getUserAgent());
                user.setTokenUpdatedAt(now);
                // to guarantee the @Version field is updated before publishing the event
                // User updated = userRepository.saveAndFlush(user);

                UserEvent evt = UserEvent.builder()
                                .eventId(UUID.randomUUID().toString())
                                .occurredAt(now)
                                .type(Type.USER_UPDATED)
                                .id(updated.getId())
                                .username(updated.getUsername())
                                .email(updated.getEmail())
                                .displayName(updated.getDisplayName())
                                .firstName(updated.getFirstName())
                                .lastName(updated.getLastName())
                                .dateJoined(updated.getCreatedAt())
                                .active(updated.isActive())
                                .emailVerified(updated.isEmailVerified())
                                .passwordAutoset(updated.isPasswordAutoset())
                                .userTimezone(updated.getUserTimezone())
                                .bot(updated.isBot())
                                .version(updated.getVersion())
                                .build();

                events.publishEvent(new UserEventToPublish(evt));

                return new AdminSigninInternalResponse(updated.getId());
        }

        Instant now = Instant.now();log.info("user IP: {}",ctx.getClientIp());
        User user = userRepository.saveAndFlush(User.create(
                        email,
                        firstName,
                        lastName,
                        passwordEncoder.encode(password),
                        UUID.randomUUID().toString().replace("-", ""),
                        false,
                        ctx.getClientIp(),
                        ctx.getUserAgent()));

        profileRepository.save(Profile.create(user,companyName));

        // user.setActive(true);
        // user.setLastActive(now);
        // user.setLastLoginTime(now);
        // user.setLastLoginIp(ctx.getClientIp());
        // user.setLastLoginUagent(ctx.getUserAgent());
        // user.setTokenUpdatedAt(now);
        // to guarantee the @Version field is updated before publishing the event
        // User updated = userRepository.saveAndFlush(user);

        UserEvent evt = UserEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .occurredAt(now)
                        .type(Type.USER_CREATED)
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
                        .build();

        events.publishEvent(new UserEventToPublish(evt));

        return new AdminSignupInternalResponse(user.getId());
    }

    @Override
    @Transactional
    public AdminSigninInternalResponse adminSignin(AdminSigninInternalRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();

        User user = userRepository.findByEmailIgnoreCase(email, User.class).orElse(null);

        if (user == null) {
            throw AuthenticationException.of(AuthErrorCode.ADMIN_USER_DOES_NOT_EXIST)
                    .with("email", email);
        }

        if (!user.isActive()) {
            throw AuthenticationException.of(AuthErrorCode.ADMIN_USER_DEACTIVATED);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw AuthenticationException.of(AuthErrorCode.ADMIN_AUTHENTICATION_FAILED);
        }

        Instant now = Instant.now();
        user.setActive(true);
        user.setLastActive(now);
        user.setLastLoginTime(now);
        user.setLastLoginIp(ctx.getClientIp());
        user.setLastLoginUagent(ctx.getUserAgent());
        user.setTokenUpdatedAt(now);
        // to guarantee the @Version field is updated before publishing the event
        User updated = userRepository.saveAndFlush(user);

        UserEvent evt = UserEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(now)
                .type(Type.USER_UPDATED)
                .id(updated.getId())
                .username(updated.getUsername())
                .email(updated.getEmail())
                .displayName(updated.getDisplayName())
                .firstName(updated.getFirstName())
                .lastName(updated.getLastName())
                .dateJoined(updated.getCreatedAt())
                .active(updated.isActive())
                .emailVerified(updated.isEmailVerified())
                .passwordAutoset(updated.isPasswordAutoset())
                .userTimezone(updated.getUserTimezone())
                .bot(updated.isBot())
                .version(updated.getVersion())
                .build();

        events.publishEvent(new UserEventToPublish(evt));

        return new AdminSigninInternalResponse(updated.getId());
    }

}
