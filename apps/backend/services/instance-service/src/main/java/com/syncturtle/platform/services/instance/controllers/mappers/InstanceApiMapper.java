package com.syncturtle.platform.services.instance.controllers.mappers;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.spring.mapping.BasicMapper;
import com.syncturtle.platform.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.platform.services.instance.dto.response.InstanceInfo;
// import com.syncturtle.platform.services.instance.configurations.properties.InstanceServiceProperties;
import com.syncturtle.platform.services.instance.dto.response.InstanceInfoResponse;
import com.syncturtle.platform.services.instance.dto.response.UserMeResponse;
import com.syncturtle.platform.services.instance.models.User;
import com.syncturtle.platform.services.instance.repositories.InstanceInfoAggregate;
import com.syncturtle.platform.services.instance.repositories.projections.InstanceAdminProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceApiMapper {

    // private final InstanceServiceProperties props;
    private final BasicMapper basicMapper;

    public InstanceInfo toInstanceInfoResponse(InstanceInfoAggregate payload) {
        InstanceInfoResponse response = new InstanceInfoResponse();

        InstanceInfoResponse.InstanceResponse instance = basicMapper.convertToResponse(payload.getInstance(),
                InstanceInfoResponse.InstanceResponse.class);

        // computed
        instance.setActivated(true);
        instance.setWorkspaceExist(payload.isWorkspacesExist());
        instance.setUserCount(payload.getUserCount());

        response.setInstance(instance);
        response.setConfig(mapConfig(payload.getConfig()));

        return response;
    }

    public UserMeResponse toInstanceAdminUserMeResponse(User user) {
        return basicMapper.convertToResponse(user, UserMeResponse.class);
    }

    public List<InstanceAdminResponse> toInstanceAdminResponseList(List<InstanceAdminProjection> payload) {
        return basicMapper.convertToResponseList(payload, InstanceAdminResponse.class);
    }

    private InstanceInfoResponse.InstanceConfigResponse mapConfig(Map<InstanceConfigurationKey, String> config) {
        InstanceInfoResponse.InstanceConfigResponse result = new InstanceInfoResponse.InstanceConfigResponse();

        result.setEnableSignup(isOn(config, InstanceConfigurationKey.ENABLE_SIGNUP));
        result.setWorkspaceCreationDisabled(isOn(config, InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION));

        result.setGoogleEnabled(isOn(config, InstanceConfigurationKey.IS_GOOGLE_ENABLED));
        result.setGithubEnabled(isOn(config, InstanceConfigurationKey.IS_GITHUB_ENABLED));
        result.setGitlabEnabled(isOn(config, InstanceConfigurationKey.IS_GITLAB_ENABLED));

        result.setMagicLoginEnabled(isOn(config, InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN));
        result.setEmailPasswordEnabled(isOn(config, InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD));

        result.setGithubAppName(config.get(InstanceConfigurationKey.GITHUB_APP_NAME));
        result.setPosthogApiKey(config.get(InstanceConfigurationKey.POSTHOG_API_KEY));
        result.setPosthogHost(config.get(InstanceConfigurationKey.POSTHOG_HOST));

        String emailHost = config.get(InstanceConfigurationKey.EMAIL_HOST);
        result.setSmtpConfigured(StringUtils.hasText(emailHost));

        // base urls should come from props

        return result;
    }

    private boolean isOn(Map<InstanceConfigurationKey, String> config, InstanceConfigurationKey key) {
        return "1".equals(config.get(key));
    }
}
