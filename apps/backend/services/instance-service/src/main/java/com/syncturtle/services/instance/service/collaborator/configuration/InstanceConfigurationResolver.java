package com.syncturtle.services.instance.service.collaborator.configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.model.InstanceConfiguration;
import com.syncturtle.services.instance.repository.InstanceConfigurationRepository;
import com.syncturtle.services.instance.service.param.RequestedKeyParam;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceConfigurationResolver {

    private final InstanceConfigurationRepository repo;
    private final InstanceConfigurationPolicy policy;
    private final InstanceConfigurationCrypto crypto;
    private final InstanceConfigurationPropertySource valueSource;

    public String resolveValue(InstanceConfigurationKey key) {
        return resolveValue(RequestedKeyParam.of(key));
    }

    public String resolveValue(RequestedKeyParam requestedKey) {
        Assert.notNull(requestedKey, "requested key is required");

        InstanceConfigurationKey key = requestedKey.getKey();
        boolean dbFirst = valueSource.skipEnvVar() || policy.preferDb(key);

        if (dbFirst) {
            return resolveDbFirst(requestedKey);
        }

        return resolvePropertiesFirst(requestedKey);
    }

    public boolean nonEmpty(InstanceConfigurationKey key) {
        return StringUtils.hasText(resolveValue(key));
    }

    public Map<InstanceConfigurationKey, String> resolveRequested(List<RequestedKeyParam> requestedKeys) {
        Assert.notNull(requestedKeys, "requested keys are required");

        Map<InstanceConfigurationKey, String> result = new LinkedHashMap<>();

        for (RequestedKeyParam requestedKey : requestedKeys) {
            Assert.notNull(requestedKey, "requested key entry is required");
            result.put(requestedKey.getKey(), resolveValue(requestedKey));
        }

        return result;
    }

    public void ensureMandatorySecretsPresentOrThrow() {
        if (!StringUtils.hasText(valueSource.secretKey())) {
            throw new IllegalStateException(
                    "Encryption secret key is required. Configure app.security.encryption.secret-key");
        }
    }

    public static int parseIntOr(String value, int fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static boolean isOn(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String normalized = value.trim();

        return "1".equals(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "yes".equalsIgnoreCase(normalized)
                || "on".equalsIgnoreCase(normalized);
    }

    private String resolveDbFirst(RequestedKeyParam requestedKey) {
        InstanceConfigurationKey key = requestedKey.getKey();
        InstanceConfiguration row = repo.findByKey(key).orElse(null);

        if (row != null) {
            return crypto.decryptIfNeeded(row);
        }

        String raw = valueSource.getRaw(key);

        if (StringUtils.hasText(raw)) {
            return raw.trim();
        }

        return fallbackFor(requestedKey);
    }

    private String resolvePropertiesFirst(RequestedKeyParam requestedKey) {
        InstanceConfigurationKey key = requestedKey.getKey();
        String raw = valueSource.getRaw(key);

        if (StringUtils.hasText(raw)) {
            return raw.trim();
        }

        InstanceConfiguration row = repo.findByKey(key).orElse(null);
        if (row != null) {
            return crypto.decryptIfNeeded(row);
        }

        return fallbackFor(requestedKey);
    }

    private String fallbackFor(RequestedKeyParam requestedKey) {
        if (requestedKey.hasDefaultOverride()) {
            return requestedKey.getDefaultValue();
        }

        return policy.defaultFor(requestedKey.getKey());
    }

}
