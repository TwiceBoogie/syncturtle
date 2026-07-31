package com.syncturtle.services.instance.service.collaborator.configuration;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.type.InstanceConfigurationCategory;

@Component
public final class InstanceConfigurationPolicy {

    private static final EnumSet<InstanceConfigurationKey> MANAGED_KEYS = EnumSet.of(
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

    private static final EnumSet<InstanceConfigurationKey> DERIVED_KEYS = EnumSet.of(
            InstanceConfigurationKey.IS_GOOGLE_ENABLED,
            InstanceConfigurationKey.IS_GITHUB_ENABLED,
            InstanceConfigurationKey.IS_GITLAB_ENABLED,
            InstanceConfigurationKey.IS_INTERCOM_ENABLED);

    private static final EnumSet<InstanceConfigurationKey> ENCRYPTED_KEYS = EnumSet.of(
            InstanceConfigurationKey.EMAIL_HOST_PASSWORD,
            InstanceConfigurationKey.GOOGLE_CLIENT_SECRET,
            InstanceConfigurationKey.GITHUB_CLIENT_SECRET,
            InstanceConfigurationKey.GITLAB_CLIENT_SECRET,
            InstanceConfigurationKey.POSTHOG_API_KEY);

    public Set<InstanceConfigurationKey> managedKeys() {
        return EnumSet.copyOf(MANAGED_KEYS);
    }

    public Set<InstanceConfigurationKey> derivedKeys() {
        return EnumSet.copyOf(DERIVED_KEYS);
    }

    public boolean isManaged(InstanceConfigurationKey key) {
        requireKey(key);
        return MANAGED_KEYS.contains(key);
    }

    public boolean isDerived(InstanceConfigurationKey key) {
        requireKey(key);
        return DERIVED_KEYS.contains(key);
    }

    /**
     * Keys whose value is *authored* by setup (computed)
     * 
     * @param key Valid instance configuration key
     * @return true or false
     */
    public boolean preferDb(InstanceConfigurationKey key) {
        requireKey(key);
        return DERIVED_KEYS.contains(key);
    }

    public boolean isEncrypted(InstanceConfigurationKey key) {
        requireKey(key);
        return ENCRYPTED_KEYS.contains(key);
    }

    public String categoryFor(InstanceConfigurationKey key) {
        requireKey(key);

        return switch (key) {
            // auth / workspace
            case ENABLE_SIGNUP,
                    ENABLE_EMAIL_PASSWORD,
                    ENABLE_MAGIC_LINK_LOGIN,
                    DISABLE_WORKSPACE_CREATION,
                    IS_GOOGLE_ENABLED,
                    IS_GITHUB_ENABLED,
                    IS_GITLAB_ENABLED ->
                InstanceConfigurationCategory.AUTHENTICATION.value();

            // smtp
            case ENABLE_SMTP,
                    EMAIL_HOST,
                    EMAIL_HOST_USER,
                    EMAIL_HOST_PASSWORD,
                    EMAIL_PORT,
                    EMAIL_FROM,
                    EMAIL_USE_TLS,
                    EMAIL_USE_SSL ->
                InstanceConfigurationCategory.SMTP.value();

            // oauth
            case GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET -> InstanceConfigurationCategory.GOOGLE.value();
            case GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, GITHUB_APP_NAME ->
                InstanceConfigurationCategory.GITHUB.value();
            case GITLAB_HOST, GITLAB_CLIENT_ID, GITLAB_CLIENT_SECRET -> InstanceConfigurationCategory.GITLAB.value();

            // intercom
            case INTERCOM_APP_ID, IS_INTERCOM_ENABLED -> InstanceConfigurationCategory.INTERCOM.value();

            // analytics
            case POSTHOG_API_KEY, POSTHOG_HOST -> InstanceConfigurationCategory.ANALYTICS.value();
        };
    }

    public String defaultFor(InstanceConfigurationKey key) {
        requireKey(key);

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

            case IS_GOOGLE_ENABLED,
                    IS_GITHUB_ENABLED,
                    IS_GITLAB_ENABLED,
                    IS_INTERCOM_ENABLED ->
                "0";
        };
    }

    public void requireManagedKey(InstanceConfigurationKey key) {
        requireKey(key);

        Assert.isTrue(isManaged(key), "Configuration key is not managed by instance-service: " + key);
    }

    public void requireDerivedKey(InstanceConfigurationKey key) {
        requireKey(key);

        Assert.isTrue(isDerived(key), "Configuration key is not a derived flag: " + key);
    }

    private static void requireKey(InstanceConfigurationKey key) {
        Assert.notNull(key, "configuration key is required");
    }
}
