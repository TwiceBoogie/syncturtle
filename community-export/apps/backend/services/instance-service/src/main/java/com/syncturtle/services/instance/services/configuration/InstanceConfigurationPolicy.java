package com.syncturtle.services.instance.services.configuration;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

@Component
public final class InstanceConfigurationPolicy {

    public Set<InstanceConfigurationKey> managedKeys() {
        return EnumSet.of(
                // auth / workspace
                InstanceConfigurationKey.ENABLE_SIGNUP,
                InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD,
                InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN,
                InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION,

                // smtp
                InstanceConfigurationKey.ENABLE_SMTP,
                InstanceConfigurationKey.EMAIL_HOST,
                InstanceConfigurationKey.EMAIL_HOST_USER,
                InstanceConfigurationKey.EMAIL_HOST_PASSWORD,
                InstanceConfigurationKey.EMAIL_PORT,
                InstanceConfigurationKey.EMAIL_FROM,
                InstanceConfigurationKey.EMAIL_USE_TLS,
                InstanceConfigurationKey.EMAIL_USE_SSL,

                // google / github / gitlab
                InstanceConfigurationKey.GOOGLE_CLIENT_ID,
                InstanceConfigurationKey.GOOGLE_CLIENT_SECRET,
                InstanceConfigurationKey.GITHUB_CLIENT_ID,
                InstanceConfigurationKey.GITHUB_CLIENT_SECRET,
                InstanceConfigurationKey.GITLAB_HOST,
                InstanceConfigurationKey.GITLAB_CLIENT_ID,
                InstanceConfigurationKey.GITLAB_CLIENT_SECRET,

                // intercom
                InstanceConfigurationKey.INTERCOM_APP_ID,

                // analytics
                InstanceConfigurationKey.POSTHOG_API_KEY,
                InstanceConfigurationKey.POSTHOG_HOST);
    }

    /**
     * Keys whose value is *authored* by setup (computed)
     * 
     * @param key Valid instance configuration key
     * @return true or false
     */
    public boolean preferDb(InstanceConfigurationKey key) {
        return switch (key) {
            case IS_GOOGLE_ENABLED, IS_GITHUB_ENABLED, IS_GITLAB_ENABLED, IS_INTERCOM_ENABLED -> true;
            default -> false;
        };
    }

    public boolean isEncrypted(InstanceConfigurationKey key) {
        return switch (key) {
            case EMAIL_HOST_PASSWORD,
                    GOOGLE_CLIENT_SECRET,
                    GITHUB_CLIENT_SECRET,
                    GITLAB_CLIENT_SECRET,
                    POSTHOG_API_KEY ->
                true;
            default -> false;
        };
    }

    public String categoryFor(InstanceConfigurationKey key) {
        return switch (key) {
            // auth / workspace
            case ENABLE_SIGNUP, ENABLE_EMAIL_PASSWORD, ENABLE_MAGIC_LINK_LOGIN,
                    DISABLE_WORKSPACE_CREATION,
                    IS_GOOGLE_ENABLED, IS_GITHUB_ENABLED, IS_GITLAB_ENABLED ->
                "AUTHENTICATION";

            // smtp
            case ENABLE_SMTP, EMAIL_HOST, EMAIL_HOST_USER, EMAIL_HOST_PASSWORD, EMAIL_PORT,
                    EMAIL_FROM, EMAIL_USE_TLS, EMAIL_USE_SSL ->
                "SMTP";

            // oauth
            case GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET -> "GOOGLE";
            case GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, GITHUB_APP_NAME -> "GITHUB";
            case GITLAB_HOST, GITLAB_CLIENT_ID, GITLAB_CLIENT_SECRET -> "GITLAB";

            // intercom
            case INTERCOM_APP_ID, IS_INTERCOM_ENABLED -> "INTERCOM";

            // analytics
            case POSTHOG_API_KEY, POSTHOG_HOST -> "ANALYTICS";
        };
    }

    public String defaultFor(InstanceConfigurationKey key) {
        return switch (key) {
            case ENABLE_SIGNUP -> "1";
            case ENABLE_EMAIL_PASSWORD -> "1";
            case ENABLE_MAGIC_LINK_LOGIN -> "0";
            case DISABLE_WORKSPACE_CREATION -> "0";

            case ENABLE_SMTP -> "0";
            case EMAIL_HOST -> "";
            case EMAIL_HOST_USER -> "";
            case EMAIL_HOST_PASSWORD -> "";
            case EMAIL_PORT -> "587";
            case EMAIL_FROM -> "";
            case EMAIL_USE_TLS -> "1";
            case EMAIL_USE_SSL -> "0";

            case GOOGLE_CLIENT_ID -> "";
            case GOOGLE_CLIENT_SECRET -> "";
            case GITHUB_CLIENT_ID -> "";
            case GITHUB_CLIENT_SECRET -> "";
            case GITHUB_APP_NAME -> "";
            case GITLAB_HOST -> "https://gitlab.com";
            case GITLAB_CLIENT_ID -> "";
            case GITLAB_CLIENT_SECRET -> "";

            case INTERCOM_APP_ID -> "";
            case POSTHOG_API_KEY -> "";
            case POSTHOG_HOST -> "";

            case IS_GOOGLE_ENABLED, IS_GITHUB_ENABLED, IS_GITLAB_ENABLED, IS_INTERCOM_ENABLED -> "0";
        };
    }
}
