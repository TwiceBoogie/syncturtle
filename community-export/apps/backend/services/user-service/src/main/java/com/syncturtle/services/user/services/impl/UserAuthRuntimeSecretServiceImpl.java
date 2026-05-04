package com.syncturtle.services.user.services.impl;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.services.user.client.InstanceClient;
import com.syncturtle.services.user.dto.internal.UserAuthRuntimeSecretConfig;
import com.syncturtle.services.user.services.UserAuthRuntimeSecretService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAuthRuntimeSecretServiceImpl implements UserAuthRuntimeSecretService {

    private static final String CACHE_KEY = "current";

    private final InstanceClient instanceClient;
    private final Cache<String, UserAuthRuntimeSecretConfig> userAuthRuntimeSecretCache;

    @Override
    public UserAuthRuntimeSecretConfig get() {
        UserAuthRuntimeSecretConfig cached = userAuthRuntimeSecretCache.getIfPresent(CACHE_KEY);
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

        userAuthRuntimeSecretCache.put(CACHE_KEY, fresh);
        return fresh;
    }

    @Override
    public void evict() {
        userAuthRuntimeSecretCache.invalidate(CACHE_KEY);
    }

}
