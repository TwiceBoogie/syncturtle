package com.syncturtle.services.user.service.impl;

import com.syncturtle.services.user.service.authentication.redirect.AuthenticationRedirector;
import com.syncturtle.services.user.service.mapper.UserApiMapper;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.user.exception.UserException;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.dto.response.IssueTokenWithUserResponse;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.messaging.outbox.UserOutboxWriter;
import com.syncturtle.services.user.model.Instance;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.repository.projection.UserPasswordAutosetProjection;
import com.syncturtle.services.user.service.AuthenticationService;
import com.syncturtle.services.user.service.authentication.provider.CredentialAuthenticationSpec;
import com.syncturtle.services.user.service.authentication.provider.CredentialAuthenticationReceipt;
import com.syncturtle.services.user.service.authentication.provider.EmailPasswordCredentialProvider;
import com.syncturtle.services.user.service.authentication.provider.MagicCodeCredentialProvider;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeSnapshot;
import com.syncturtle.services.user.service.session.AuthenticatedSessionIssueSpec;
import com.syncturtle.services.user.service.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.session.RefreshSessionTokenStore;
import com.syncturtle.services.user.type.AuthenticationFlowType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AuthenticationRedirector authenticationRedirector;
    private final InstanceRepository instanceRepository;
    private final UserRepository userRepository;
    private final RequestClientContext clientContext;
    private final UserAuthRuntimeConfigResolver configFlagResolver;
    private final EmailPasswordCredentialProvider emailPasswordProvider;
    private final MagicCodeCredentialProvider magicCodeProvider;
    private final AuthenticatedSessionIssuer authenticatedSessionIssuer;
    private final RefreshSessionTokenStore refreshSessionTokenStore;
    private final PasswordEncoder passwordEncoder;
    private final UserEventFactory userEventFactory;
    private final UserOutboxWriter userOutboxWriter;
    private final UserApiMapper userApiMapper;

    @Override
    @Transactional(readOnly = true)
    public EmailCheckResponse emailCheck(String email) {
        requireInstanceSetup();

        String normalizedEmail = normalizeEmailForLookup(email);

        UserAuthRuntimeSnapshot configurations = configFlagResolver.getInstanceConfigurations();
        Assert.notNull(configurations, "user auth runtime config is required");

        boolean magicCodeAvailable = configurations.isSignupEnabled() && configurations.isMagicLinkEnabled();

        return userRepository.findByEmailIgnoreCase(normalizedEmail, UserPasswordAutosetProjection.class)
                .map(user -> {
                    AuthenticationFlowType authenticationFlow = user.isPasswordAutoset() && magicCodeAvailable
                            ? AuthenticationFlowType.MAGIC_CODE
                            : AuthenticationFlowType.CREDENTIAL;
                    return EmailCheckResponse.forExistingUser(authenticationFlow);
                })
                .orElseGet(() -> {
                    AuthenticationFlowType authenticationFlow = magicCodeAvailable
                            ? AuthenticationFlowType.MAGIC_CODE
                            : AuthenticationFlowType.CREDENTIAL;
                    return EmailCheckResponse.forNewUser(authenticationFlow);
                });
    }

    @Override
    @Transactional
    public IssueTokenResponse emailPasswordSignIn(String email, String password, String nextPath) {
        try {
            Instance instance = requireInstanceSetup();

            requireEmailPasswordInput(email, password, AuthErrorCode.REQUIRED_EMAIL_PASSWORD_SIGN_IN);

            CredentialAuthenticationReceipt result = emailPasswordProvider.authenticate(
                    CredentialAuthenticationSpec.builder()
                            .email(email)
                            .secret(password)
                            .signup(false)
                            .ipAddress(clientContext.getClientIp())
                            .userAgent(clientContext.getUserAgent())
                            .build());

            return issueTokenResponse(result.getUser(), instance.getId(), nextPath);
        } catch (AuthException exception) {
            return authenticationRedirector.signInFailure(exception, nextPath);
        }
    }

    @Override
    @Transactional
    public IssueTokenResponse emailPasswordSignUp(String email, String password, String nextPath) {
        try {
            Instance instance = requireInstanceSetup();

            requireEmailPasswordInput(email, password, AuthErrorCode.REQUIRED_EMAIL_PASSWORD_SIGN_UP);

            CredentialAuthenticationReceipt result = emailPasswordProvider.authenticate(
                    CredentialAuthenticationSpec.builder()
                            .email(email)
                            .secret(password)
                            .signup(true)
                            .ipAddress(clientContext.getClientIp())
                            .userAgent(clientContext.getUserAgent())
                            .build());

            return issueTokenResponse(result.getUser(), instance.getId(), nextPath);
        } catch (AuthException exception) {
            return authenticationRedirector.signUpFailure(exception, nextPath);
        }
    }

    @Override
    @Transactional
    public IssueTokenResponse magicCodeSignIn(String email, String code, String nextPath) {
        try {
            Instance instance = requireInstanceSetup();

            // requireMagicCodeInput(email, code,
            // AuthErrorCode.MAGIC_SIGN_IN_EMAIL_CODE_REQUIRED);

            CredentialAuthenticationReceipt result = magicCodeProvider.authenticate(
                    CredentialAuthenticationSpec.builder()
                            .email(email)
                            .secret(code)
                            .signup(false)
                            .ipAddress(clientContext.getClientIp())
                            .userAgent(clientContext.getUserAgent())
                            .build());

            return issueTokenResponse(result.getUser(), instance.getId(), nextPath);
        } catch (AuthException exception) {
            return authenticationRedirector.signInFailure(exception, nextPath);
        }
    }

    @Override
    @Transactional
    public IssueTokenResponse magicCodeSignUp(String email, String code, String nextPath) {
        try {
            Instance instance = requireInstanceSetup();

            // requireMagicCodeInput(email, code,
            // AuthErrorCode.MAGIC_SIGN_UP_EMAIL_CODE_REQUIRED);

            CredentialAuthenticationReceipt result = magicCodeProvider.authenticate(
                    CredentialAuthenticationSpec.builder()
                            .email(email)
                            .secret(code)
                            .signup(true)
                            .ipAddress(clientContext.getClientIp())
                            .userAgent(clientContext.getUserAgent())
                            .build());

            return issueTokenResponse(result.getUser(), instance.getId(), nextPath);
        } catch (AuthException exception) {
            return authenticationRedirector.signUpFailure(exception, nextPath);
        }
    }

    @Override
    @Transactional
    public String signOut(UUID currentUserId, String logoutContext, String sessionId) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.hasText(logoutContext, "logout context is required");
        Assert.notNull(sessionId, "sessionId is required");

        refreshSessionTokenStore.revokeSession(sessionId);
        return authenticationRedirector.signOutRedirect(logoutContext).getRedirection();
    }

    @Override
    @Transactional
    public IssueTokenWithUserResponse setPassword(UUID currentUserId, String sessionId, String password) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(sessionId, "sessionId is required");

        Instance instance = requireInstanceSetup();

        User user = userRepository.findById(currentUserId).orElseThrow(() -> UserException.userNotFound(currentUserId));

        // If user password is not autoset then return error
        if (!user.isPasswordAutoset()) {
            throw AuthException.of(AuthErrorCode.PASSWORD_ALREADY_SET)
                    .with("error", "Your password is already set please change your password from profile");
        }

        requireAcceptablePassword(password);

        String passwordhash = passwordEncoder.encode(password);
        user.markPasswordChanged(passwordhash, false);
        userRepository.flush();

        refreshSessionTokenStore.revokeUserSessions(currentUserId.toString());

        UserEvent event = userEventFactory.updated(user);
        userOutboxWriter.saveUserEvent(event);

        return IssueTokenWithUserResponse.builder()
                .tokens(issueTokenResponse(user, instance.getId(), null))
                .user(userApiMapper.toMe(user))
                .build();
    }

    private Instance requireInstanceSetup() {
        Instance instance = instanceRepository.findFirstByOrderByCreatedAtAsc().orElse(null);

        if (instance == null || !instance.isSetupDone()) {
            throw AuthException.of(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
        }

        return instance;
    }

    private void requireAcceptablePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw AuthException.of(AuthErrorCode.INVALID_PASSWORD);
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw AuthException.of(AuthErrorCode.INVALID_PASSWORD);
        }

        // TODO: plug zxcvbn4j later
    }

    private IssueTokenResponse issueTokenResponse(User user, UUID instanceId, String nextPath) {
        Assert.notNull(user, "user is required");
        Assert.notNull(instanceId, "instanceId is required");

        AuthenticatedSessionReceipt session = authenticatedSessionIssuer.issueNewSession(
                AuthenticatedSessionIssueSpec.builder()
                        .user(user)
                        .instanceId(instanceId)
                        .ipAddress(clientContext.getClientIp())
                        .userAgent(clientContext.getUserAgent())
                        .build());

        return IssueTokenResponse.issued(session, authenticationRedirector.successLocation(nextPath));
    }

    private static void requireEmailPasswordInput(String email, String password, AuthErrorCode errorCode) {
        Assert.notNull(errorCode, "errorCode is required");

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw AuthException.of(errorCode).with("email", email);
        }
    }

    private static String normalizeEmailForLookup(String email) {
        if (!StringUtils.hasText(email)) {
            throw AuthException.of(AuthErrorCode.INVALID_EMAIL);
        }

        String normalized = email.trim().toLowerCase();

        if (!normalized.contains("@")) {
            throw AuthException.of(AuthErrorCode.INVALID_EMAIL)
                    .with("email", email);
        }

        return normalized;
    }

}
