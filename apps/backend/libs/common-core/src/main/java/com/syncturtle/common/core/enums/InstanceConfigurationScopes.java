package com.syncturtle.common.core.enums;

public final class InstanceConfigurationScopes {

    private InstanceConfigurationScopes() {
    }

    public static InstanceConfigScope scopeOf(InstanceConfigurationKey key) {
        return switch (key) {
            case ENABLE_SIGNUP,
                    ENABLE_MAGIC_LINK_LOGIN,
                    ENABLE_EMAIL_PASSWORD ->
                InstanceConfigScope.AUTH;

            case ENABLE_SMTP,
                    EMAIL_HOST,
                    EMAIL_HOST_USER,
                    EMAIL_HOST_PASSWORD,
                    EMAIL_PORT,
                    EMAIL_FROM,
                    EMAIL_USE_TLS,
                    EMAIL_USE_SSL ->
                InstanceConfigScope.EMAIL;

            case IS_GOOGLE_ENABLED,
                    GOOGLE_CLIENT_ID,
                    GOOGLE_CLIENT_SECRET,
                    IS_GITHUB_ENABLED,
                    GITHUB_APP_NAME,
                    GITHUB_CLIENT_ID,
                    GITHUB_CLIENT_SECRET,
                    IS_GITLAB_ENABLED,
                    GITLAB_HOST,
                    GITLAB_CLIENT_ID,
                    GITLAB_CLIENT_SECRET ->
                InstanceConfigScope.SOCIAL_LOGIN;

            case IS_INTERCOM_ENABLED,
                    INTERCOM_APP_ID,
                    POSTHOG_API_KEY,
                    POSTHOG_HOST ->
                InstanceConfigScope.TELEMETRY;

            case DISABLE_WORKSPACE_CREATION -> InstanceConfigScope.WORKSPACE;
        };
    }

}
