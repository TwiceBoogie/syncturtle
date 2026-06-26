package com.syncturtle.services.user.service.runtime;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeConfigResponse;
import com.syncturtle.services.user.client.InstanceClient;
import com.syncturtle.services.user.configuration.cache.UserAuthRuntimeCacheNames;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAuthRuntimeConfigResolver {

    private final InstanceClient instanceClient;

    @Cacheable(cacheNames = UserAuthRuntimeCacheNames.USER_AUTH_RUNTIME, key = UserAuthRuntimeCacheNames.CURRENT_SPEL_KEY, sync = true)
    public UserAuthRuntimeSnapshot getInstanceConfigurations() {
        UserAuthRuntimeConfigResponse response = instanceClient.getUserAuthRuntimeConfig();

        Assert.notNull(response, "User auth runtime config response must not be null");
        Assert.notNull(response.getVersion(), "User auth runtime config version must not be null");

        return UserAuthRuntimeSnapshot.builder()
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

    @CacheEvict(cacheNames = UserAuthRuntimeCacheNames.USER_AUTH_RUNTIME, key = UserAuthRuntimeCacheNames.CURRENT_SPEL_KEY)
    public void evict() {
        // annotation does the eviction for us
    }

}
