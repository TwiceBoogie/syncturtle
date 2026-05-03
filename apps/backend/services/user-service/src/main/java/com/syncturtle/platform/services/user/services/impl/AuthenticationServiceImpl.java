package com.syncturtle.platform.services.user.services.impl;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.exceptions.AuthenticationException;
import com.syncturtle.common.spring.web.url.HostUrlBuilder;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.platform.services.user.dto.internal.UserAuthRuntimeConfig;
import com.syncturtle.platform.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.platform.services.user.models.Instance;
import com.syncturtle.platform.services.user.models.User;
import com.syncturtle.platform.services.user.repositories.InstanceRepository;
import com.syncturtle.platform.services.user.repositories.UserRepository;
import com.syncturtle.platform.services.user.repositories.projections.UserPasswordAutosetProjection;
import com.syncturtle.platform.services.user.services.AuthenticationService;
import com.syncturtle.platform.services.user.services.FeatureFlagService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    // context
    private final RequestUserContext userContext;
    private final RequestClientContext clientContext;
    // host resolver
    private final HostUrlBuilder hostResolver;
    // repositories
    private final InstanceRepository instanceRepository;
    private final UserRepository userRepository;
    // services
    private final FeatureFlagService featureFlagService;

    @Override
    @Transactional(readOnly = true)
    public EmailCheckResponse emailCheck(String email) {
        Instance instance = instanceRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
        if (instance == null || !instance.isSetupDone()) {
            throw new AuthenticationException(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
        }

        UserAuthRuntimeConfig configurations = featureFlagService.getInstanceConfigurations();

        boolean smtpConfigured = configurations.isSmtpEnabled();
        boolean isMagicLoginEnabled = configurations.isMagicLinkEnabled();

        email = email.trim().toLowerCase();

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
    public String signOut(String logoutContext) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(logoutContext);
        String redirectionUrl = isAdmin ? hostResolver.baseHost(true, false, false)
                : hostResolver.baseHost(false, false, true);

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

}
