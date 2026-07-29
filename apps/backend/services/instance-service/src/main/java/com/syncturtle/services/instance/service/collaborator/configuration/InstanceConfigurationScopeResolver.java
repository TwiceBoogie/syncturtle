package com.syncturtle.services.instance.service.collaborator.configuration;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

public final class InstanceConfigurationScopeResolver {

    private static final Map<InstanceConfigurationKey, InstanceConfigurationScope> KEY_TO_SCOPE = new EnumMap<>(
            InstanceConfigurationKey.class);

    static {
        // AUTH
        put(InstanceConfigurationKey.ENABLE_SIGNUP, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.ENABLE_SMTP, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.IS_GOOGLE_ENABLED, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.IS_GITHUB_ENABLED, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.IS_GITLAB_ENABLED, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.GOOGLE_CLIENT_ID, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.GITHUB_CLIENT_ID, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.GITHUB_CLIENT_SECRET, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.GITLAB_HOST, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.GITLAB_CLIENT_ID, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.GITLAB_CLIENT_SECRET, InstanceConfigurationScope.AUTH);
        put(InstanceConfigurationKey.GITHUB_APP_NAME, InstanceConfigurationScope.AUTH);

        // EMAIL
        put(InstanceConfigurationKey.EMAIL_HOST, InstanceConfigurationScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_HOST_USER, InstanceConfigurationScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_HOST_PASSWORD, InstanceConfigurationScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_PORT, InstanceConfigurationScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_FROM, InstanceConfigurationScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_USE_TLS, InstanceConfigurationScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_USE_SSL, InstanceConfigurationScope.EMAIL);

        // WORKSPACE POLICY
        put(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION, InstanceConfigurationScope.WORKSPACE);

        // ANALYTICS
        put(InstanceConfigurationKey.IS_INTERCOM_ENABLED, InstanceConfigurationScope.ANALYTICS);
        put(InstanceConfigurationKey.INTERCOM_APP_ID, InstanceConfigurationScope.ANALYTICS);
        put(InstanceConfigurationKey.POSTHOG_API_KEY, InstanceConfigurationScope.ANALYTICS);
        put(InstanceConfigurationKey.POSTHOG_HOST, InstanceConfigurationScope.ANALYTICS);
    }

    private InstanceConfigurationScopeResolver() {
    }

    private static void put(InstanceConfigurationKey key, InstanceConfigurationScope scope) {
        KEY_TO_SCOPE.put(key, scope);
    }

    public static InstanceConfigurationScope scopeOf(InstanceConfigurationKey key) {
        InstanceConfigurationScope scope = KEY_TO_SCOPE.get(key);
        if (scope == null) {
            throw new IllegalArgumentException("No scope mapping exists for key: " + key);
        }
        return scope;
    }

    public static EnumSet<InstanceConfigurationScope> scopesOf(Set<InstanceConfigurationKey> keys) {
        EnumSet<InstanceConfigurationScope> scopes = EnumSet.noneOf(InstanceConfigurationScope.class);
        for (InstanceConfigurationKey key : keys) {
            scopes.add(scopeOf(key));
        }
        return scopes;
    }

}
