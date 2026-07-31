package com.syncturtle.services.user.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.configuration.property.AuthProperties;
import com.syncturtle.services.user.messaging.kafka.factory.AuthenticationEmailEventFactory;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.model.InstanceLite;
import com.syncturtle.services.user.model.PasswordResetToken;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceLiteRepository;
import com.syncturtle.services.user.repository.PasswordResetTokenRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.PasswordResetService;
import com.syncturtle.services.user.service.collaborator.authentication.password.PasswordResetUidCodec;
import com.syncturtle.services.user.service.collaborator.authentication.password.PasswordResetUrlBuilder;
import com.syncturtle.services.user.service.collaborator.outbox.EmailOutboxWriter;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSnapshot;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionTokenStore;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int MAX_PRESENTED_TOKEN_LENGTH = 256;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 256;

    private final InstanceLiteRepository instanceRepository;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserAuthRuntimeConfigResolver configFlagResolver;
    private final PasswordResetUidCodec uidCodec;
    private final PasswordResetUrlBuilder resetUrlBuilder;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final AuthProperties authProperties;
    private final PasswordEncoder passwordEncoder;
    private final RefreshSessionTokenStore refreshSessionTokenStore;
    private final AuthenticationEmailEventFactory emailEventFactory;
    private final EmailOutboxWriter emailOutboxWriter;
    private final UserEventFactory userEventFactory;
    private final UserOutboxWriter userOutboxWriter;
    private final Clock clock;

    @Override
    @Transactional
    public void requestReset(String email) {
        requireInstanceSetup();
        requireSmtpConfigured();

        String normalizedEmail = normalizeEmailForLookup(email);

        User user = userRepository.findByEmailIgnoreCaseForUpdate(normalizedEmail).orElse(null);

        if (user == null) {
            return;
        }

        Instant now = clock.instant();

        invalidateOutstandingTokens(user.getId(), now);

        String rawToken = tokenGenerator.generateBase64Url(authProperties.getPasswordResetTokenBytes());
        String tokenHash = tokenHasher.hash(rawToken);
        Instant expiresAt = now.plus(authProperties.getPasswordResetTokenTtl());

        PasswordResetToken passwordResetToken = PasswordResetToken.issue(user, tokenHash, expiresAt);

        passwordResetTokenRepository.save(passwordResetToken);

        String uidb64 = uidCodec.encode(user.getId());
        String resetUrl = resetUrlBuilder.build(uidb64, rawToken);

        EmailToSendEvent emailEvent = emailEventFactory.passwordReset(normalizedEmail, resetUrl,
                authProperties.getPasswordResetTokenTtl());
        emailOutboxWriter.saveEmailToSendEvent(emailEvent, user.getId());
    }

    @Override
    @Transactional
    public void resetPassword(String uidb64, String rawToken, String password) {
        requirePassword(password);

        UUID userId = decodeUserId(uidb64);
        if (!StringUtils.hasText(rawToken) || rawToken.length() > MAX_PRESENTED_TOKEN_LENGTH) {
            throw invalidPasswordToken();
        }

        User user = userRepository.findByIdForUpdate(userId).orElseThrow(this::invalidPasswordToken);

        String tokenHash = tokenHasher.hash(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByUserIdAndTokenHashForUpdate(userId, tokenHash)
                .orElseThrow(this::invalidPasswordToken);

        Instant now = clock.instant();
        verifyResetToken(resetToken, now);

        List<PasswordResetToken> outstandingTokens = passwordResetTokenRepository
                .findOutstandingByUserIdForUpdate(userId);
        String passwordHash = passwordEncoder.encode(password);

        resetToken.consume(now);

        invalidateOtherTokens(outstandingTokens, resetToken, now);

        user.markPasswordChanged(passwordHash, false);

        userRepository.flush();

        refreshSessionTokenStore.revokeUserSessions(user.getId().toString());

        UserEvent userEvent = userEventFactory.updated(user);
        userOutboxWriter.saveUserEvent(userEvent);
    }

    private InstanceLite requireInstanceSetup() {
        InstanceLite instance = instanceRepository.findFirstByOrderByCreatedAtAsc().orElse(null);

        if (instance == null || !instance.isSetupDone()) {
            throw AuthException.of(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
        }

        return instance;
    }

    private void requireSmtpConfigured() {
        UserAuthRuntimeSnapshot configuration = configFlagResolver.getInstanceConfigurations();

        Assert.notNull(configuration, "user auth runtime config is required");

        if (!configuration.isSmtpEnabled()) {
            throw AuthException.of(AuthErrorCode.SMTP_NOT_CONFIGURED);
        }
    }

    private void invalidateOutstandingTokens(UUID userId, Instant invalidatedAt) {
        Assert.notNull(userId, "userId is required");
        Assert.notNull(invalidatedAt, "invalidatedAt is required");

        List<PasswordResetToken> outstandingTokens = passwordResetTokenRepository
                .findOutstandingByUserIdForUpdate(userId);

        for (PasswordResetToken outstandingToken : outstandingTokens) {
            outstandingToken.invalidate(invalidatedAt);
        }
    }

    private void invalidateOtherTokens(List<PasswordResetToken> outstandingTokens, PasswordResetToken consumedToken,
            Instant invalidatedAt) {
        Assert.notNull(outstandingTokens, "outstandingTokens is required");
        Assert.notNull(consumedToken, "consumedToken is required");
        Assert.notNull(invalidatedAt, "invalidatedAt is required");

        for (PasswordResetToken outstandingToken : outstandingTokens) {
            if (outstandingToken.getId().equals(consumedToken.getId())) {
                continue;
            }

            outstandingToken.invalidate(invalidatedAt);
        }
    }

    private void verifyResetToken(PasswordResetToken resetToken, Instant now) {
        Assert.notNull(resetToken, "passwordResetToken is required");

        Assert.notNull(now, "now is required");

        if (resetToken.isExpired(now)) {
            throw AuthException.of(AuthErrorCode.EXPIRED_PASSWORD_TOKEN);
        }

        if (resetToken.isConsumed()
                || resetToken.isInvalidated()
                || resetToken.isDeleted()
                || !resetToken.matchesCurrentAuthVersion()) {

            throw invalidPasswordToken();
        }
    }

    private String normalizeEmailForLookup(String email) {
        if (!StringUtils.hasText(email)) {
            throw AuthException.of(AuthErrorCode.INVALID_EMAIL);
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (!normalizedEmail.contains("@")) {
            throw AuthException.of(AuthErrorCode.INVALID_EMAIL)
                    .with("email", email);
        }

        return normalizedEmail;
    }

    private UUID decodeUserId(String uidb64) {
        if (!StringUtils.hasText(uidb64)) {
            throw invalidPasswordToken();
        }

        return uidCodec.decode(uidb64).orElseThrow(this::invalidPasswordToken);
    }

    private AuthException invalidPasswordToken() {
        return AuthException.of(AuthErrorCode.INVALID_PASSWORD_TOKEN);
    }

    private static void requirePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw AuthException.of(AuthErrorCode.INVALID_PASSWORD);
        }

        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw AuthException.of(AuthErrorCode.INVALID_PASSWORD);
        }

        // TODO: Integrate zxcvbn4j
    }

}
