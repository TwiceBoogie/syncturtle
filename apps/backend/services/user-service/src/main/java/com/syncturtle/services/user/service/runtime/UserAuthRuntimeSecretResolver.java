package com.syncturtle.services.user.service.runtime;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.services.user.client.InstanceClient;
import com.syncturtle.services.user.configuration.cache.UserAuthRuntimeCacheNames;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAuthRuntimeSecretResolver {

    private final InstanceClient instanceClient;
    private final Cache<String, UserAuthRuntimeSecretConfig> userAuthRuntimeSecretCache;

    public UserAuthRuntimeSecretConfig get() {
        UserAuthRuntimeSecretConfig cached = userAuthRuntimeSecretCache
                .getIfPresent(UserAuthRuntimeCacheNames.CURRENT_KEY);
        if (cached != null) {
            return cached;
        }

        UserAuthRuntimeSecretConfigResponse response = instanceClient.getUserAuthRuntimeSecretConfig();

        UserAuthRuntimeSecretConfig fresh = UserAuthRuntimeSecretConfig.builder()
                .googleClientId(response.getGoogleClientId())
                .googleClientSecret(response.getGoogleClientSecret())
                .githubClientId(response.getGithubClientId())
                .githubClientSecret(response.getGithubClientSecret())
                .githubAppName(response.getGithubAppName())
                .gitlabHost(response.getGitlabHost())
                .gitlabClientId(response.getGitlabClientId())
                .gitlabClientSecret(response.getGitlabClientSecret())
                .version(response.getVersion())
                .build();

        userAuthRuntimeSecretCache.put(UserAuthRuntimeCacheNames.CURRENT_KEY, fresh);
        return fresh;
    }

    public void evict() {
        userAuthRuntimeSecretCache.invalidate(UserAuthRuntimeCacheNames.CURRENT_KEY);
    }

}
