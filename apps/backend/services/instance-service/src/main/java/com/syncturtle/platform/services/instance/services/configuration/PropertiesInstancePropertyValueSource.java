package com.syncturtle.platform.services.instance.services.configuration;

import org.springframework.stereotype.Component;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.configurations.properties.InstanceServiceProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class PropertiesInstancePropertyValueSource implements InstancePropertyValueSource {

    private final InstanceServiceProperties props;

    @Override
    public boolean skipEnvVar() {
        return props.isSkipEnvVar();
    }

    @Override
    public String getRaw(InstanceConfigurationKey key) {
        InstanceServiceProperties.ConfigKeys c = props.getConfigKeys();
        if (c == null) {
            return null;
        }
        return switch (key) {
            // auth / workspace
            case ENABLE_SIGNUP -> String.valueOf(c.getEnableSignup());
            case ENABLE_EMAIL_PASSWORD -> String.valueOf(c.getEnableEmailPassword());
            case ENABLE_MAGIC_LINK_LOGIN -> String.valueOf(c.getEnableMagicLinkLogin());
            case DISABLE_WORKSPACE_CREATION -> String.valueOf(c.getDisableWorkspaceCreation());

            // smtp
            case ENABLE_SMTP -> String.valueOf(c.getEnableSmtp());
            case EMAIL_HOST -> c.getEmailHost();
            case EMAIL_HOST_USER -> c.getEmailHostUser();
            case EMAIL_HOST_PASSWORD -> c.getEmailHostPassword();
            case EMAIL_PORT -> String.valueOf(c.getEmailPort());
            case EMAIL_FROM -> c.getEmailFrom();
            case EMAIL_USE_TLS -> String.valueOf(c.getEmailUseTls());
            case EMAIL_USE_SSL -> String.valueOf(c.getEmailUseSsl());

            // oauth
            case GOOGLE_CLIENT_ID -> c.getGoogleClientId();
            case GOOGLE_CLIENT_SECRET -> c.getGoogleClientSecret();
            case GITHUB_CLIENT_ID -> c.getGithubClientId();
            case GITHUB_CLIENT_SECRET -> c.getGithubClientSecret();

            case GITLAB_HOST -> c.getGitlabHost();
            case GITLAB_CLIENT_ID -> c.getGitlabClientId();
            case GITLAB_CLIENT_SECRET -> c.getGitlabClientSecret();

            // intercom / analytics
            case INTERCOM_APP_ID -> c.getIntercomAppId();
            case POSTHOG_API_KEY -> c.getPosthogApiKey();
            case POSTHOG_HOST -> c.getPosthogHost();

            // derived flags + any keys not represented in app.* config
            case IS_GOOGLE_ENABLED,
                    IS_GITHUB_ENABLED,
                    IS_GITLAB_ENABLED,
                    IS_INTERCOM_ENABLED,
                    GITHUB_APP_NAME ->
                null;
        };
    }

    @Override
    public String secretKey() {
        if (props.getSecurity() == null || props.getSecurity().getEncryption() == null) {
            return null;
        }
        return props.getSecurity().getEncryption().getSecretKey();
    }

}
