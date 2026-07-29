package com.syncturtle.services.email.service.collaborator;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.github.benmanes.caffeine.cache.Cache;
import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.services.email.client.InstanceClient;
import com.syncturtle.services.email.exception.EmailRuntimeConfigException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailRuntimeConfigResolver {

    private static final String CACHE_KEY = "current";

    private final InstanceClient instanceClient;
    private final Cache<String, EmailRuntimeConfigSnapshot> cache;

    private final Object refreshMonitor = new Object();

    public EmailRuntimeConfigSnapshot resolveCurrent() {
        EmailRuntimeConfigSnapshot cached = cache.getIfPresent(CACHE_KEY);

        if (cached != null) {
            return cached;
        }

        synchronized (refreshMonitor) {
            EmailRuntimeConfigSnapshot secondCheck = cache.getIfPresent(CACHE_KEY);

            if (secondCheck != null) {
                return secondCheck;
            }

            EmailRuntimeConfigSnapshot fresh = fetchFresh();
            cache.put(CACHE_KEY, fresh);

            return fresh;
        }
    }

    public EmailRuntimeConfigSnapshot refreshIfOlderThan(long requiredScopeVersion) {
        Assert.isTrue(requiredScopeVersion >= 0, "requiredScopeVersion must not be negative");

        EmailRuntimeConfigSnapshot cached = cache.getIfPresent(CACHE_KEY);

        if (cached != null && cached.getVersion() >= requiredScopeVersion) {
            return cached;
        }

        synchronized (refreshMonitor) {
            EmailRuntimeConfigSnapshot secondCheck = cache.getIfPresent(CACHE_KEY);

            if (secondCheck != null && secondCheck.getVersion() >= requiredScopeVersion) {
                return secondCheck;
            }

            EmailRuntimeConfigSnapshot fresh = fetchFresh();

            if (fresh.getVersion() < requiredScopeVersion) {
                throw EmailRuntimeConfigException.stale(fresh.getVersion(), requiredScopeVersion);
            }

            cache.put(CACHE_KEY, fresh);

            return fresh;
        }
    }

    public void evict() {
        cache.invalidate(CACHE_KEY);
    }

    private EmailRuntimeConfigSnapshot fetchFresh() {
        try {
            EmailRuntimeSecretConfigResponse response = instanceClient.getRuntimeEmailConfig();

            Assert.notNull(response, "runtime email configuration response is required");

            return EmailRuntimeConfigSnapshot.builder()
                    .enabled(response.isEnabled())
                    .host(response.getHost())
                    .port(response.getPort())
                    .username(response.getUsername())
                    .password(response.getPassword())
                    .from(response.getFrom())
                    .useTls(response.isUseTls())
                    .useSsl(response.isUseSsl())
                    .version(response.getVersion())
                    .build();
        } catch (Exception exception) {
            throw EmailRuntimeConfigException.fetchFailed(exception);
        }
    }

}
