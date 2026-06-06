package com.syncturtle.services.user.service.authentication.workflow;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.services.user.model.Profile;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.model.param.UserCreateParam;
import com.syncturtle.services.user.repository.ProfileRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeConfigResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CredentialAuthenticationWorkflow {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserAuthRuntimeConfigResolver configFlagResolver;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public User completeLoginOrSignup(CredentialCompletionSpec param) {
        Assert.notNull(param, "credential completion param is required");

        CredentialUserDataSpec userData = param.getUserData();
        String email = normalizeEmail(userData.getEmail());

        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email, User.class);

        boolean createdUser = false;
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            requireSignupEnabled(email);
            user = createUser(userData, param);
            createdUser = true;
        }

        user.recordSuccessfullLogin(param.getProvider().value(), param.getIpAddress(), param.getUserAgent(), clock);
        User savedUser = userRepository.saveAndFlush(user);

        if (createdUser) {
            Profile profile = Profile.create(user, clock, "");
            profileRepository.save(profile);
        }

        // Later: publish to kafka

        return savedUser;
    }

    private User createUser(CredentialUserDataSpec userDataParam, CredentialCompletionSpec completionParam) {
        String email = normalizeEmail(userDataParam.getEmail());

        String passwordHash;
        boolean passwordAutoset;

        if (userDataParam.isPasswordAutoset()) {
            passwordHash = passwordEncoder.encode(UUID.randomUUID().toString());
            passwordAutoset = true;
        } else {
            validatePasswordStrength(userDataParam.getRawPassword(), email);
            passwordHash = passwordEncoder.encode(userDataParam.getRawPassword());
            passwordAutoset = false;
        }

        UserCreateParam userCreateParam = UserCreateParam.builder()
                .username(UUID.randomUUID().toString().replace("-", ""))
                .email(email)
                .displayName(resolveDisplayName(userDataParam))
                .firstName(userDataParam.getFirstName())
                .lastName(userDataParam.getLastName())
                .passwordHash(passwordHash)
                .passwordAutoset(passwordAutoset)
                .userTimezone("America/Chicago")
                .initialLoginIp(completionParam.getIpAddress())
                .initialLoginMedium(completionParam.getProvider().value())
                .initialLoginUserAgent(completionParam.getUserAgent())
                .build();

        return User.create(userCreateParam, clock);
    }

    private void requireSignupEnabled(String email) {
        boolean signupEnabled = configFlagResolver.getInstanceConfigurations().isSignupEnabled();

        if (!signupEnabled) {
            throw AuthException.of(AuthErrorCode.SIGNUP_DISABLED)
                    .with("email", email);
        }
    }

    private void validatePasswordStrength(String rawPassword, String email) {
        Assert.hasText(rawPassword, "rawPassword is required");

        // plug zxcvbn4j later
        if (rawPassword.length() < 8) {
            throw AuthException.of(AuthErrorCode.INVALID_NEW_PASSWORD)
                    .with("email", email);
        }
    }

    private String resolveDisplayName(CredentialUserDataSpec userData) {
        if (hasText(userData.getDisplayName())) {
            return userData.getDisplayName().trim();
        }

        if (hasText(userData.getFirstName()) || hasText(userData.getLastName())) {
            return ((userData.getFirstName() == null ? "" : userData.getFirstName()) + " "
                    + (userData.getLastName() == null ? "" : userData.getLastName())).trim();
        }

        String email = normalizeEmail(userData.getEmail());
        int atIndex = email.indexOf("@");

        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }

        return email;
    }

    private String normalizeEmail(String email) {
        Assert.hasText(email, "email is required");

        String normalized = email.trim().toLowerCase();
        if (!normalized.contains("@")) {
            throw AuthException.of(AuthErrorCode.INVALID_EMAIL);
        }

        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
