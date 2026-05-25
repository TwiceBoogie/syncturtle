package com.syncturtle.services.email.service;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.services.email.clients.InstanceClient;
import com.syncturtle.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.services.email.exceptions.EmailRuntimeConfigException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailRuntimeConfigService {

    private static final String CACHE_KEY = "current";

    private final InstanceClient instanceClient;
    private final Cache<String, EmailRuntimeConfig> cache;
    private final Object refreshMonitor = new Object();

    public EmailRuntimeConfig getCurrentConfig() {
        EmailRuntimeConfig cached = cache.getIfPresent(CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        synchronized (refreshMonitor) {
            EmailRuntimeConfig secondCheck = cache.getIfPresent(CACHE_KEY);
            if (secondCheck != null) {
                return secondCheck;
            }

            EmailRuntimeConfig fresh = fetchFresh();
            cache.put(CACHE_KEY, fresh);
            return fresh;
        }
    }

    public EmailRuntimeConfig refreshIfOlderThan(long requiredScopeVersion) {
        EmailRuntimeConfig cached = cache.getIfPresent(CACHE_KEY);
        if (cached != null && cached.getVersion() >= requiredScopeVersion) {
            return cached;
        }

        synchronized (refreshMonitor) {
            EmailRuntimeConfig secondCheck = cache.getIfPresent(CACHE_KEY);
            if (secondCheck != null && secondCheck.getVersion() >= requiredScopeVersion) {
                return secondCheck;
            }

            EmailRuntimeConfig fresh = fetchFresh();
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

    private EmailRuntimeConfig fetchFresh() {
        try {
            EmailRuntimeSecretConfigResponse response = instanceClient.getRuntimeEmailConfig();

            return EmailRuntimeConfig.builder()
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
