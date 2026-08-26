package com.syncturtle.services.user.service.collaborator.runtime;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.github.benmanes.caffeine.cache.Cache;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.services.user.client.InstanceClient;
import com.syncturtle.services.user.configuration.cache.UserAuthRuntimeCacheNames;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAuthRuntimeSecretResolver {

    private static final int MAX_FETCH_ATTEMPTS = 2;

    private final InstanceClient instanceClient;
    private final Cache<String, UserAuthRuntimeSecretConfig> cache;
    private final Object loadMonitor = new Object();
    private final AtomicLong highestRequiredVersion = new AtomicLong();

    public UserAuthRuntimeSecretConfig get() {
        UserAuthRuntimeSecretConfig cached = cached();

        if (cached != null) {
            return cached;
        }

        synchronized (loadMonitor) {
            UserAuthRuntimeSecretConfig secondCheck = cached();

            if (secondCheck != null) {
                return secondCheck;
            }

            UserAuthRuntimeSecretConfig fresh = fetch();
            cache.put(UserAuthRuntimeCacheNames.CURRENT_KEY, fresh);
            return fresh;
        }
    }

    public UserAuthRuntimeSecretConfig refreshIfOlderThan(long requiredConfigurationVersion) {
        Assert.isTrue(requiredConfigurationVersion >= 0,
                "requiredConfigurationVersion must not be negative");

        highestRequiredVersion.accumulateAndGet(requiredConfigurationVersion, Math::max);
        UserAuthRuntimeSecretConfig cached = cached();

        if (satisfies(cached, requiredConfigurationVersion)) {
            return cached;
        }

        synchronized (loadMonitor) {
            for (int attempt = 1; attempt <= MAX_FETCH_ATTEMPTS; attempt++) {
                long target = highestRequiredVersion.get();

                UserAuthRuntimeSecretConfig secondCheck = cached();
                if (satisfies(secondCheck, target)) {
                    return secondCheck;
                }

                evict();
                UserAuthRuntimeSecretConfig fresh = fetch();
                long latestTarget = highestRequiredVersion.get();

                if (satisfies(fresh, latestTarget)) {
                    cache.put(UserAuthRuntimeCacheNames.CURRENT_KEY, fresh);
                    return fresh;
                }

                if (attempt == MAX_FETCH_ATTEMPTS) {
                    throw new IllegalStateException(
                            "User auth secret snapshot version %s does not satisfy required version %s"
                                    .formatted(fresh.getVersion(), latestTarget));
                }
            }
            throw new IllegalStateException("Unreachable user auth secret refresh state");
        }
    }

    public void evict() {
        cache.invalidate(UserAuthRuntimeCacheNames.CURRENT_KEY);
    }

    private UserAuthRuntimeSecretConfig cached() {
        return cache.getIfPresent(UserAuthRuntimeCacheNames.CURRENT_KEY);
    }

    private UserAuthRuntimeSecretConfig fetch() {
        UserAuthRuntimeSecretConfigResponse response = instanceClient.getUserAuthRuntimeSecretConfig();

        Assert.notNull(response, "User auth runtime secret response must not be null");
        Assert.notNull(response.getVersion(), "User auth runtime secret version must not be null");
        Assert.isTrue(response.getVersion() >= 0, "User auth runtime secret version must not be negative");

        return UserAuthRuntimeSecretConfig.builder()
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
    }

    private static boolean satisfies(UserAuthRuntimeSecretConfig config, long required) {
        return config != null && config.getVersion() != null && config.getVersion() >= required;
    }

}
