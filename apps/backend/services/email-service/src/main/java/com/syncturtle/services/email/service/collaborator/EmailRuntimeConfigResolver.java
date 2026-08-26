package com.syncturtle.services.email.service.collaborator;

import java.util.concurrent.atomic.AtomicLong;

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
    private static final int MAX_FETCH_ATTEMPTS = 2;

    private final InstanceClient instanceClient;
    private final Cache<String, EmailRuntimeConfigSnapshot> cache;
    private final Object refreshMonitor = new Object();
    // stores the highest config version requested by any caller
    private final AtomicLong highestRequiredVersion = new AtomicLong();

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

    public EmailRuntimeConfigSnapshot refreshIfOlderThan(long requiredConfigurationVersion) {
        Assert.isTrue(requiredConfigurationVersion >= 0, "requiredConfigurationVersion must not be negative");

        highestRequiredVersion.accumulateAndGet(requiredConfigurationVersion, Math::max);

        EmailRuntimeConfigSnapshot cached = cache.getIfPresent(CACHE_KEY);
        if (cached != null && cached.getVersion() >= requiredConfigurationVersion) {
            return cached;
        }

        synchronized (refreshMonitor) {
            for (int attempt = 1; attempt <= MAX_FETCH_ATTEMPTS; attempt++) {
                long target = highestRequiredVersion.get();
                EmailRuntimeConfigSnapshot secondCheck = cache.getIfPresent(CACHE_KEY);
                if (secondCheck != null && secondCheck.getVersion() >= target) {
                    return secondCheck;
                }

                cache.invalidate(CACHE_KEY);
                EmailRuntimeConfigSnapshot fresh = fetchFresh();
                long latestTarget = highestRequiredVersion.get();
                if (fresh.getVersion() >= latestTarget) {
                    cache.put(CACHE_KEY, fresh);
                    return fresh;
                }
                if (attempt == MAX_FETCH_ATTEMPTS) {
                    throw EmailRuntimeConfigException.stale(fresh.getVersion(), latestTarget);
                }
            }
            throw new IllegalStateException("Unreachable email runtime configuration refresh state");
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
        } catch (EmailRuntimeConfigException exception) {
            throw exception;
        } catch (Exception exception) {
            throw EmailRuntimeConfigException.fetchFailed(exception);
        }
    }

}
