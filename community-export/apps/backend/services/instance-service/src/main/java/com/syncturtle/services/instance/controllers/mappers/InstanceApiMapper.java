package com.syncturtle.services.instance.controllers.mappers;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.spring.mapping.BasicMapper;
import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.services.instance.dto.response.InstanceInfo;
import com.syncturtle.services.instance.dto.response.InstanceInfoResponse;
import com.syncturtle.services.instance.dto.response.InstanceResponse;
import com.syncturtle.services.instance.dto.response.UserMeResponse;
import com.syncturtle.services.instance.models.User;
import com.syncturtle.services.instance.payload.InstanceSummary;
import com.syncturtle.services.instance.payload.InstanceSummaryWithConfig;
import com.syncturtle.services.instance.repositories.projections.InstanceAdminProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceApiMapper {

    // private final InstanceServiceProperties props;
    private final BasicMapper basicMapper;

    public InstanceInfo toInstanceInfoResponse(InstanceSummaryWithConfig payload) {
        InstanceInfoResponse response = new InstanceInfoResponse();

        InstanceResponse instance = toInstanceResponse(payload);

        response.setInstance(instance);
        response.setConfig(mapConfig(payload.getConfigurations()));

        return response;
    }

    public InstanceResponse toInstanceResponse(InstanceSummary payload) {
        InstanceResponse instance = basicMapper.convertToResponse(payload.getInstance(), InstanceResponse.class);
        // fields that are embeded therefore BasicMapper would miss
        instance.setCurrentVersion(payload.getInstance().getUpdateCheck().getCurrentVersion());
        instance.setLatestVersion(payload.getInstance().getUpdateCheck().getLatestVersion());
        instance.setLastCheckedAt(payload.getInstance().getUpdateCheck().getLastCheckedAt());
        instance.setNamespace(payload.getInstance().getRuntime().getNamespace());

        // computed
        instance.setActivated(true);
        instance.setUserCount(payload.getUserCount());
        instance.setWorkspaceExist(payload.isWorkspaceExist());

        return instance;
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
