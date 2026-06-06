package com.syncturtle.services.instance.util;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScopeNames;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

public final class InstanceConfigurationScope {

    private static final Map<InstanceConfigurationKey, InstanceConfigurationScopeNames> KEY_TO_SCOPE = new EnumMap<>(
            InstanceConfigurationKey.class);

    static {
        // AUTH
        put(InstanceConfigurationKey.ENABLE_SIGNUP, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.ENABLE_SMTP, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.IS_GOOGLE_ENABLED, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.IS_GITHUB_ENABLED, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.IS_GITLAB_ENABLED, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.GOOGLE_CLIENT_ID, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.GITHUB_CLIENT_ID, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.GITHUB_CLIENT_SECRET, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.GITLAB_HOST, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.GITLAB_CLIENT_ID, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.GITLAB_CLIENT_SECRET, InstanceConfigurationScopeNames.AUTH);
        put(InstanceConfigurationKey.GITHUB_APP_NAME, InstanceConfigurationScopeNames.AUTH);

        // EMAIL
        put(InstanceConfigurationKey.EMAIL_HOST, InstanceConfigurationScopeNames.EMAIL);
        put(InstanceConfigurationKey.EMAIL_HOST_USER, InstanceConfigurationScopeNames.EMAIL);
        put(InstanceConfigurationKey.EMAIL_HOST_PASSWORD, InstanceConfigurationScopeNames.EMAIL);
        put(InstanceConfigurationKey.EMAIL_PORT, InstanceConfigurationScopeNames.EMAIL);
        put(InstanceConfigurationKey.EMAIL_FROM, InstanceConfigurationScopeNames.EMAIL);
        put(InstanceConfigurationKey.EMAIL_USE_TLS, InstanceConfigurationScopeNames.EMAIL);
        put(InstanceConfigurationKey.EMAIL_USE_SSL, InstanceConfigurationScopeNames.EMAIL);

        // WORKSPACE POLICY
        put(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION, InstanceConfigurationScopeNames.WORKSPACE);

        // ANALYTICS
        put(InstanceConfigurationKey.IS_INTERCOM_ENABLED, InstanceConfigurationScopeNames.ANALYTICS);
        put(InstanceConfigurationKey.INTERCOM_APP_ID, InstanceConfigurationScopeNames.ANALYTICS);
        put(InstanceConfigurationKey.POSTHOG_API_KEY, InstanceConfigurationScopeNames.ANALYTICS);
        put(InstanceConfigurationKey.POSTHOG_HOST, InstanceConfigurationScopeNames.ANALYTICS);
    }

    private InstanceConfigurationScope() {
    }

    private static void put(InstanceConfigurationKey key, InstanceConfigurationScopeNames scope) {
        KEY_TO_SCOPE.put(key, scope);
    }

    public static InstanceConfigurationScopeNames scopeOf(InstanceConfigurationKey key) {
        InstanceConfigurationScopeNames scope = KEY_TO_SCOPE.get(key);
        if (scope == null) {
            throw new IllegalArgumentException("No scope mapping exists for key: " + key);
        }
        return scope;
    }

    public static EnumSet<InstanceConfigurationScopeNames> scopesOf(Set<InstanceConfigurationKey> keys) {
        EnumSet<InstanceConfigurationScopeNames> scopes = EnumSet.noneOf(InstanceConfigurationScopeNames.class);
        for (InstanceConfigurationKey key : keys) {
            scopes.add(scopeOf(key));
        }
        return scopes;
    }

}
