package com.syncturtle.platform.services.instance.services.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.models.InstanceConfiguration;
import com.syncturtle.platform.services.instance.repositories.InstanceConfigurationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceConfigurationResolver {

    private final InstanceConfigurationRepository repo;
    private final InstanceConfigurationPolicy policy;
    private final InstanceConfigurationCrypto crypto;
    private final InstancePropertyValueSource valueSource;

    public String resolveValue(InstanceConfigurationKey key) {
        return resolveValue(key, null);
    }

    public String resolveValue(InstanceConfigurationKey key, String defaultOverride) {
        boolean preferDb = valueSource.skipEnvVar() || policy.preferDb(key);

        if (preferDb) {
            InstanceConfiguration row = repo.findByKey(key).orElse(null);
            if (row != null) {
                return crypto.decryptIfNeeded(row);
            }

            // DB missing -> fallback to app.* then default
            String raw = valueSource.getRaw(key);
            if (StringUtils.hasText(raw)) {
                return raw;
            }

            return defaultOverride != null ? defaultOverride : policy.defaultFor(key);
        }

        // normal mode -> app.* first
        String raw = valueSource.getRaw(key);
        if (StringUtils.hasText(raw)) {
            return raw;
        }

        return defaultOverride != null ? defaultOverride : policy.defaultFor(key);
    }

    public boolean nonEmpty(InstanceConfigurationKey key) {
        return StringUtils.hasText(resolveValue(key));
    }

    public static int parseIntOr(String value, int d) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception e) {
            return d;
        }
    }

    public void ensureMandatorySecretsPresentOrThrow() {
        if (!StringUtils.hasText(valueSource.secretKey())) {
            throw new IllegalStateException("SECRET_KEY is required (app.security.encryption.secret-key");
        }
    }

    public Map<InstanceConfigurationKey, String> resolveRequested(List<RequestedKey> keys) {
        Map<InstanceConfigurationKey, String> m = new HashMap<>();
        for (RequestedKey key : keys) {
            m.put(key.key(), resolveValue(key.key(), key.defaultValue()));
        }
        return m;
    }

    public record RequestedKey(InstanceConfigurationKey key, String defaultValue) {
        public static RequestedKey of(InstanceConfigurationKey key) {
            return new RequestedKey(key, null);
        }

        public static RequestedKey of(InstanceConfigurationKey key, String defaultValue) {
            return new RequestedKey(key, defaultValue);
        }
    }
}
