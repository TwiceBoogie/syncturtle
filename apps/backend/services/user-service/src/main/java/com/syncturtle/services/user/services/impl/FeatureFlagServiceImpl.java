package com.syncturtle.services.user.services.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeConfigResponse;
import com.syncturtle.services.user.client.InstanceClient;
import com.syncturtle.services.user.dto.internal.UserAuthRuntimeConfig;
import com.syncturtle.services.user.services.FeatureFlagService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureFlagServiceImpl implements FeatureFlagService {

    private final InstanceClient instanceClient;

    @Override
    @Cacheable(cacheNames = "s2s:instance:user-auth-runtime", key = "'current'", unless = "#result == null", sync = true)
    public UserAuthRuntimeConfig getInstanceConfigurations() {
        UserAuthRuntimeConfigResponse response = instanceClient.getUserAuthRuntimeConfig();

        return UserAuthRuntimeConfig.builder()
                .signupEnabled(response.isSignupEnabled())
                .magicLinkEnabled(response.isMagicLinkEnabled())
                .emailPasswordEnabled(response.isEmailPasswordEnabled())
                .smtpEnabled(response.isSmtpEnabled())
                .googleEnabled(response.isGoogleEnabled())
                .githubEnabled(response.isGithubEnabled())
                .gitlabEnabled(response.isGitlabEnabled())
                .version(response.getVersion())
                .build();
    }

    @Override
    @CacheEvict(cacheNames = "s2s:instance:user-auth-runtime", key = "'current'")
    public void evict() {
        // annotation does the eviction for us
    }

}
