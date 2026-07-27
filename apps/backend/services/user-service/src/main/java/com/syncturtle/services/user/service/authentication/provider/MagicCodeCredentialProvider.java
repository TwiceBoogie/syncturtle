package com.syncturtle.services.user.service.authentication.provider;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.authentication.magic.MagicCodeChallenge;
import com.syncturtle.services.user.service.authentication.magic.MagicCodeStore;
import com.syncturtle.services.user.service.authentication.workflow.CredentialAuthenticationWorkflow;
import com.syncturtle.services.user.service.authentication.workflow.CredentialCompletionSpec;
import com.syncturtle.services.user.service.authentication.workflow.CredentialUserDataSpec;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.type.CredentialProviderType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MagicCodeCredentialProvider implements CredentialProvider {

    private final UserRepository userRepository;
    private final UserAuthRuntimeConfigResolver configFlagResolver;
    private final MagicCodeStore magicCodeStore;
    private final CredentialAuthenticationWorkflow workflow;

    @Override
    public CredentialProviderType provider() {
        return CredentialProviderType.MAGIC_CODE;
    }

    public MagicCodeChallenge initiate(String email) {
        Assert.hasText(email, "email is required");

        requireMagicLinkEnabled(email);
        requireSmtpConfigured(email);

        boolean existingUser = userRepository.existsByEmailIgnoreCase(email);

        AuthErrorCode attemptExhaustedErrorCode = existingUser
                ? AuthErrorCode.EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_IN
                : AuthErrorCode.EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_UP;
        return magicCodeStore.createOrRotate(email, attemptExhaustedErrorCode);
    }

    @Override
    public CredentialAuthenticationReceipt authenticate(CredentialAuthenticationSpec param) {
        Assert.notNull(param, "credential authentication param is required");

        requireSubmittedCode(param);
        requireMagicLinkEnabled(param.getEmail());
        requireSmtpConfigured(param.getEmail());

        boolean existingUser = userRepository.existsByEmailIgnoreCase(param.getEmail());

        requireRequestedFlowMatchesUserState(param, param.getEmail(), existingUser);

        MagicCodeStore.VerificationResult verification = magicCodeStore.verify(param.getEmail(), param.getSecret());

        if (!verification.isVerified()) {
            throwMagicCodeFailure(param.getEmail(), verification.getFailureReason(), existingUser);
        }

        User user = workflow.completeLoginOrSignup(CredentialCompletionSpec.builder()
                .provider(provider())
                .userData(CredentialUserDataSpec.builder()
                        .email(param.getEmail())
                        .firstName("")
                        .lastName("")
                        .displayName(null)
                        .avatarUrl("")
                        .passwordAutoset(true)
                        .rawPassword(null)
                        .build())
                .ipAddress(param.getIpAddress())
                .userAgent(param.getUserAgent())
                .build());

        return existingUser
                ? CredentialAuthenticationReceipt.existingUser(user)
                : CredentialAuthenticationReceipt.createdUser(user);
    }

    private void requireRequestedFlowMatchesUserState(CredentialAuthenticationSpec param, String email,
            boolean existingUser) {
        if (param.isSignup() && existingUser) {
            throw AuthException.of(AuthErrorCode.USER_ALREADY_EXISTS);
        }
        if (!param.isSignup() && !existingUser) {
            throw AuthException.of(AuthErrorCode.USER_DOES_NOT_EXIST).with("email", email);
        }
    }

    private void requireSubmittedCode(CredentialAuthenticationSpec param) {
        if (StringUtils.hasText(param.getSecret())) {
            return;
        }

        AuthErrorCode errorCode = param.isSignup()
                ? AuthErrorCode.MAGIC_SIGN_UP_EMAIL_CODE_REQUIRED
                : AuthErrorCode.MAGIC_SIGN_IN_EMAIL_CODE_REQUIRED;

        throw AuthException.of(errorCode).with("email", param.getEmail());
    }

    private void requireMagicLinkEnabled(String email) {
        boolean enabled = configFlagResolver.getInstanceConfigurations().isMagicLinkEnabled();

        if (!enabled) {
            throw AuthException.of(AuthErrorCode.MAGIC_LINK_LOGIN_DISABLED)
                    .with("email", email);
        }
    }

    private void requireSmtpConfigured(String email) {
        boolean smtpEnabled = configFlagResolver.getInstanceConfigurations().isSmtpEnabled();

        if (!smtpEnabled) {
            throw AuthException.of(AuthErrorCode.SMTP_NOT_CONFIGURED)
                    .with("email", email);
        }
    }

    private void throwMagicCodeFailure(String email, MagicCodeStore.FailureReason reason, boolean existingUser) {
        if (reason == MagicCodeStore.FailureReason.EXPIRED) {
            throw AuthException.of(existingUser
                    ? AuthErrorCode.EXPIRED_MAGIC_CODE_SIGN_IN
                    : AuthErrorCode.EXPIRED_MAGIC_CODE_SIGN_UP)
                    .with("email", email);
        }

        throw AuthException.of(existingUser
                ? AuthErrorCode.INVALID_MAGIC_CODE_SIGN_IN
                : AuthErrorCode.INVALID_MAGIC_CODE_SIGN_UP)
                .with("email", email);
    }

}
