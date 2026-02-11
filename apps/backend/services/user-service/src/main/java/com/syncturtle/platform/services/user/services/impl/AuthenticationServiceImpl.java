package com.syncturtle.platform.services.user.services.impl;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.core.exceptions.AuthenticationException;
import com.syncturtle.platform.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.platform.services.user.models.Instance;
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

        Map<InstanceConfigurationKey, String> configurations = featureFlagService.getInstanceConfigurations();

        boolean smtpConfigured = bool(configurations.get(InstanceConfigurationKey.EMAIL_HOST));
        boolean isMagicLoginEnabled = bool(configurations.get(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN));

        email = email.trim().toLowerCase();

        Optional<UserPasswordAutosetProjection> user = userRepository.findByEmailIgnoreCase(email);
        if (user.isPresent()) {
            boolean shouldMagic = user.get().isPasswordAutoset() && smtpConfigured && isMagicLoginEnabled;
            return new EmailCheckResponse(shouldMagic ? "MAGIC_CODE" : "CREDENTIAL", true, false);
        }
        boolean shouldMagic = smtpConfigured && isMagicLoginEnabled;
        return new EmailCheckResponse(shouldMagic ? "MAGIC_CODE" : "CREDENTIAL", false, false);
    }

    private static boolean bool(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        if ("0".equals(s)) {
            return false;
        }

        return true;
    }

}
