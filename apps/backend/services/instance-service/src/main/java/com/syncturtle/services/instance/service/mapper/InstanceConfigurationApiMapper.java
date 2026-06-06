package com.syncturtle.services.instance.service.mapper;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.web.properties.PublicUrlProperties;
import com.syncturtle.services.instance.dto.response.InstanceSetupConfigResponse;

@Component
public final class InstanceConfigurationApiMapper {

    public InstanceSetupConfigResponse toResponse(Map<InstanceConfigurationKey, String> config,
            PublicUrlProperties props) {
        return InstanceSetupConfigResponse.builder()
                .enableSignup(isOn(config, InstanceConfigurationKey.ENABLE_SIGNUP))
                .workspaceCreationDisabled(isOn(config, InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION))
                .googleEnabled(isOn(config, InstanceConfigurationKey.IS_GOOGLE_ENABLED))
                .githubEnabled(isOn(config, InstanceConfigurationKey.IS_GITHUB_ENABLED))
                .gitlabEnabled(isOn(config, InstanceConfigurationKey.IS_GITLAB_ENABLED))
                .magicLoginEnabled(isOn(config, InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN))
                .emailPasswordEnabled(isOn(config, InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD))
                .githubAppName(config.get(InstanceConfigurationKey.GITHUB_APP_NAME))
                .posthogApiKey(config.get(InstanceConfigurationKey.POSTHOG_API_KEY))
                .posthogHost(config.get(InstanceConfigurationKey.POSTHOG_HOST))
                // .fileSizeLimit(config.get(InstanceConfigurationKey.EMAIL_FROM))
                .smtpConfigured(isOn(config, InstanceConfigurationKey.ENABLE_SMTP))
                .intercomEnabled(isOn(config, InstanceConfigurationKey.IS_INTERCOM_ENABLED))
                .intercomAppId(config.get(InstanceConfigurationKey.INTERCOM_APP_ID))
                .adminBaseUrl(props.getAdmin().getOrigin())
                .appBaseUrl(props.getUserApp().getOrigin())
                .instanceChangelogUrl(props.getInstanceChangelogUrl())
                .build();
    }

    private static boolean isOn(Map<InstanceConfigurationKey, String> config, InstanceConfigurationKey key) {
        return "1".equals(config.get(key));
    }

}
