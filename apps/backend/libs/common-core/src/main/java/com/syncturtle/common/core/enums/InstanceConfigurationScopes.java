package com.syncturtle.common.core.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class InstanceConfigurationScopes {

    private static final Map<InstanceConfigurationKey, InstanceConfigScope> KEY_TO_SCOPE = new EnumMap<>(
            InstanceConfigurationKey.class);

    static {
        // AUTH
        put(InstanceConfigurationKey.ENABLE_SIGNUP, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.ENABLE_SMTP, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.IS_GOOGLE_ENABLED, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.IS_GITHUB_ENABLED, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.IS_GITLAB_ENABLED, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.GOOGLE_CLIENT_ID, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.GITHUB_CLIENT_ID, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.GITHUB_CLIENT_SECRET, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.GITLAB_HOST, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.GITLAB_CLIENT_ID, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.GITLAB_CLIENT_SECRET, InstanceConfigScope.AUTH);
        put(InstanceConfigurationKey.GITHUB_APP_NAME, InstanceConfigScope.AUTH);

        // EMAIL
        put(InstanceConfigurationKey.EMAIL_HOST, InstanceConfigScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_HOST_USER, InstanceConfigScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_HOST_PASSWORD, InstanceConfigScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_PORT, InstanceConfigScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_FROM, InstanceConfigScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_USE_TLS, InstanceConfigScope.EMAIL);
        put(InstanceConfigurationKey.EMAIL_USE_SSL, InstanceConfigScope.EMAIL);

        // WORKSPACE POLICY
        put(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION, InstanceConfigScope.WORKSPACE);

        // ANALYTICS
        put(InstanceConfigurationKey.IS_INTERCOM_ENABLED, InstanceConfigScope.ANALYTICS);
        put(InstanceConfigurationKey.INTERCOM_APP_ID, InstanceConfigScope.ANALYTICS);
        put(InstanceConfigurationKey.POSTHOG_API_KEY, InstanceConfigScope.ANALYTICS);
        put(InstanceConfigurationKey.POSTHOG_HOST, InstanceConfigScope.ANALYTICS);
    }

    private InstanceConfigurationScopes() {
    }

    private static void put(InstanceConfigurationKey key, InstanceConfigScope scope) {
        KEY_TO_SCOPE.put(key, scope);
    }

    public static InstanceConfigScope scopeOf(InstanceConfigurationKey key) {
        InstanceConfigScope scope = KEY_TO_SCOPE.get(key);
        if (scope == null) {
            throw new IllegalArgumentException("No scope mapping exists for key: " + key);
        }
        return scope;
    }

    public static EnumSet<InstanceConfigScope> scopesOf(Set<InstanceConfigurationKey> keys) {
        EnumSet<InstanceConfigScope> scopes = EnumSet.noneOf(InstanceConfigScope.class);
        for (InstanceConfigurationKey key : keys) {
            scopes.add(scopeOf(key));
        }
        return scopes;
    }

}
