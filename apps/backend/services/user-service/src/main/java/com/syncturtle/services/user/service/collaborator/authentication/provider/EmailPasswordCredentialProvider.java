package com.syncturtle.services.user.service.collaborator.authentication.provider;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.collaborator.authentication.workflow.CredentialAuthenticationWorkflow;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.param.CredentialAuthenticationParam;
import com.syncturtle.services.user.service.param.CredentialCompletionParam;
import com.syncturtle.services.user.service.param.CredentialUserDataParam;
import com.syncturtle.services.user.type.CredentialProviderType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailPasswordCredentialProvider implements CredentialProvider {

    private final UserRepository userRepository;
    private final UserAuthRuntimeConfigResolver configFlagResolver;
    private final PasswordEncoder passwordEncoder;
    private final CredentialAuthenticationWorkflow workflow;

    @Override
    public CredentialProviderType provider() {
        return CredentialProviderType.EMAIL_PASSWORD;
    }

    @Override
    public CredentialAuthenticationReceipt authenticate(CredentialAuthenticationParam param) {
        Assert.notNull(param, "credential authentication param is required");

        requireEmailPasswordAuthenticationEnabled(param.getEmail());

        if (param.isSignup()) {
            requireUserDoesNotExist(param.getEmail());

            User user = workflow.completeLoginOrSignup(CredentialCompletionParam.builder()
                    .provider(provider())
                    .userData(CredentialUserDataParam.builder()
                            .email(param.getEmail())
                            .firstName("")
                            .lastName("")
                            .displayName(null)
                            .avatarUrl("")
                            .passwordAutoset(false)
                            .rawPassword(param.getSecret())
                            .build())
                    .ipAddress(param.getIpAddress())
                    .userAgent(param.getUserAgent())
                    .build());

            return CredentialAuthenticationReceipt.createdUser(user);
        }

        User existingUser = requireExistingUser(param.getEmail());
        requirePasswordMatches(existingUser, param.getSecret());

        User user = workflow.completeLoginOrSignup(CredentialCompletionParam.builder()
                .provider(provider())
                .userData(CredentialUserDataParam.builder()
                        .email(param.getEmail())
                        .firstName(existingUser.getFirstName())
                        .lastName(existingUser.getLastName())
                        .displayName(existingUser.getDisplayName())
                        .avatarUrl("")
                        .passwordAutoset(false)
                        .rawPassword(param.getSecret())
                        .build())
                .ipAddress(param.getIpAddress())
                .userAgent(param.getUserAgent())
                .build());

        return CredentialAuthenticationReceipt.existingUser(user);
    }

    private void requireEmailPasswordAuthenticationEnabled(String email) {
        boolean enabled = configFlagResolver.getInstanceConfigurations().isEmailPasswordEnabled();

        if (!enabled) {
            throw AuthException.of(AuthErrorCode.EMAIL_PASSWORD_AUTHENTICATION_DISABLED)
                    .with("email", email);
        }
    }

    private User requireExistingUser(String email) {
        return userRepository.findByEmailIgnoreCase(email, User.class)
                .orElseThrow(() -> AuthException.of(AuthErrorCode.USER_DOES_NOT_EXIST)
                        .with("email", email));
    }

    private void requireUserDoesNotExist(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw AuthException.of(AuthErrorCode.USER_ALREADY_EXISTS)
                    .with("email", email);
        }
    }

    private void requirePasswordMatches(User user, String rawPassword) {
        Assert.notNull(user, "user is required");

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED_SIGN_IN)
                    .with("email", user.getEmail());
        }
    }

}
