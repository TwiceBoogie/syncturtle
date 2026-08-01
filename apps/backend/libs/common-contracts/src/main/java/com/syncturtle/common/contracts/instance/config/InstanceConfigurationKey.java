package com.syncturtle.common.contracts.instance.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum InstanceConfigurationKey {
    ENABLE_SIGNUP(InstanceConfigurationScope.AUTH),
    ENABLE_MAGIC_LINK_LOGIN(InstanceConfigurationScope.AUTH),
    ENABLE_EMAIL_PASSWORD(InstanceConfigurationScope.AUTH),
    ENABLE_SMTP(InstanceConfigurationScope.AUTH, InstanceConfigurationScope.EMAIL),
    DISABLE_WORKSPACE_CREATION(InstanceConfigurationScope.WORKSPACE),
    IS_GOOGLE_ENABLED(InstanceConfigurationScope.AUTH),
    IS_GITHUB_ENABLED(InstanceConfigurationScope.AUTH),
    IS_GITLAB_ENABLED(InstanceConfigurationScope.AUTH),
    GITHUB_APP_NAME(InstanceConfigurationScope.AUTH),
    EMAIL_HOST(InstanceConfigurationScope.EMAIL),
    EMAIL_HOST_USER(InstanceConfigurationScope.EMAIL),
    EMAIL_HOST_PASSWORD(InstanceConfigurationScope.EMAIL),
    EMAIL_PORT(InstanceConfigurationScope.EMAIL),
    EMAIL_FROM(InstanceConfigurationScope.EMAIL),
    EMAIL_USE_TLS(InstanceConfigurationScope.EMAIL),
    EMAIL_USE_SSL(InstanceConfigurationScope.EMAIL),
    GOOGLE_CLIENT_ID(InstanceConfigurationScope.AUTH),
    GOOGLE_CLIENT_SECRET(InstanceConfigurationScope.AUTH),
    GITHUB_CLIENT_ID(InstanceConfigurationScope.AUTH),
    GITHUB_CLIENT_SECRET(InstanceConfigurationScope.AUTH),
    GITLAB_HOST(InstanceConfigurationScope.AUTH),
    GITLAB_CLIENT_ID(InstanceConfigurationScope.AUTH),
    GITLAB_CLIENT_SECRET(InstanceConfigurationScope.AUTH),
    IS_INTERCOM_ENABLED(InstanceConfigurationScope.ANALYTICS),
    INTERCOM_APP_ID(InstanceConfigurationScope.ANALYTICS),
    POSTHOG_API_KEY(InstanceConfigurationScope.ANALYTICS),
    POSTHOG_HOST(InstanceConfigurationScope.ANALYTICS);

    private final Set<InstanceConfigurationScope> scopes;

    InstanceConfigurationKey(InstanceConfigurationScope first, InstanceConfigurationScope... remaining) {
        EnumSet<InstanceConfigurationScope> configured = EnumSet.of(first, remaining);
        this.scopes = Collections.unmodifiableSet(configured);
    }

    public Set<InstanceConfigurationScope> scopes() {
        return scopes;
    }

    public boolean belongsTo(InstanceConfigurationScope scope) {
        return scope != null && scopes.contains(scope);
    }

    public static InstanceConfigurationKey parseOrNull(String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(key -> key.name().equals(value))
                .findFirst()
                .orElse(null);
    }
}
