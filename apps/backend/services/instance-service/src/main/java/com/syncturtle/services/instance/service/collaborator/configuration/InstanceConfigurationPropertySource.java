package com.syncturtle.services.instance.service.collaborator.configuration;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.configuration.property.InstanceServiceProperties;

@Component
public final class InstanceConfigurationPropertySource {

    private final InstanceServiceProperties props;

    public InstanceConfigurationPropertySource(InstanceServiceProperties props) {
        Assert.notNull(props, "instance service properties are required");
        this.props = props;
    }

    public boolean skipEnvVar() {
        return props.isSkipEnvVar();
    }

    public String getRaw(InstanceConfigurationKey key) {
        Assert.notNull(key, "configuration key is required");

        InstanceServiceProperties.ConfigKeys configKeys = props.getConfigKeys();
        if (configKeys == null) {
            return null;
        }

        return switch (key) {
            // auth / workspace
            case ENABLE_SIGNUP -> valueOf(configKeys.getEnableSignup());
            case ENABLE_EMAIL_PASSWORD -> valueOf(configKeys.getEnableEmailPassword());
            case ENABLE_MAGIC_LINK_LOGIN -> valueOf(configKeys.getEnableMagicLinkLogin());
            case DISABLE_WORKSPACE_CREATION -> valueOf(configKeys.getDisableWorkspaceCreation());

            // smtp
            case ENABLE_SMTP -> valueOf(configKeys.getEnableSmtp());
            case EMAIL_HOST -> configKeys.getEmailHost();
            case EMAIL_HOST_USER -> configKeys.getEmailHostUser();
            case EMAIL_HOST_PASSWORD -> configKeys.getEmailHostPassword();
            case EMAIL_PORT -> valueOf(configKeys.getEmailPort());
            case EMAIL_FROM -> configKeys.getEmailFrom();
            case EMAIL_USE_TLS -> valueOf(configKeys.getEmailUseTls());
            case EMAIL_USE_SSL -> valueOf(configKeys.getEmailUseSsl());

            // oauth
            case GOOGLE_CLIENT_ID -> configKeys.getGoogleClientId();
            case GOOGLE_CLIENT_SECRET -> configKeys.getGoogleClientSecret();
            case GITHUB_CLIENT_ID -> configKeys.getGithubClientId();
            case GITHUB_CLIENT_SECRET -> configKeys.getGithubClientSecret();

            case GITLAB_HOST -> configKeys.getGitlabHost();
            case GITLAB_CLIENT_ID -> configKeys.getGitlabClientId();
            case GITLAB_CLIENT_SECRET -> configKeys.getGitlabClientSecret();

            // intercom / analytics
            case INTERCOM_APP_ID -> configKeys.getIntercomAppId();
            case POSTHOG_API_KEY -> configKeys.getPosthogApiKey();
            case POSTHOG_HOST -> configKeys.getPosthogHost();

            // derived flags + any keys not represented in app.* config
            case IS_GOOGLE_ENABLED,
                    IS_GITHUB_ENABLED,
                    IS_GITLAB_ENABLED,
                    IS_INTERCOM_ENABLED,
                    GITHUB_APP_NAME ->
                null;
        };
    }

    public String secretKey() {
        if (props.getSecurity() == null) {
            return null;
        }

        if (props.getSecurity().getEncryption() == null) {
            return null;
        }

        return props.getSecurity().getEncryption().getSecretKey();
    }

    @SuppressWarnings("unused")
    private static String valueOf(Boolean value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String valueOf(Integer value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unused")
    private static String valueOf(Long value) {
        return value == null ? null : String.valueOf(value);
    }

}
