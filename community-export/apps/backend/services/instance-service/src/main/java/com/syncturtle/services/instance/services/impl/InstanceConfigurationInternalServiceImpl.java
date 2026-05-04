package com.syncturtle.services.instance.services.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeConfigResponse;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.workspace.config.WorkspaceRuntimeConfigResponse;
import com.syncturtle.services.instance.models.Instance;
import com.syncturtle.services.instance.repositories.InstanceRepository;
import com.syncturtle.services.instance.services.InstanceConfigurationInternalService;
import com.syncturtle.services.instance.services.configuration.InstanceConfigurationResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceConfigurationInternalServiceImpl implements InstanceConfigurationInternalService {

    private final InstanceRepository instanceRepository;
    private final InstanceConfigurationResolver resolver;

    @Override
    @Transactional(readOnly = true)
    public EmailRuntimeSecretConfigResponse getEmailRuntimeSecretConfig() {
        Instance instance = latestInstance();

        Map<InstanceConfigurationKey, String> values = resolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_SMTP),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST_USER),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST_PASSWORD),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_PORT),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_FROM),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_USE_TLS),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_USE_SSL)));

        return EmailRuntimeSecretConfigResponse.builder()
                .enabled(isOn(values.get(InstanceConfigurationKey.ENABLE_SMTP)))
                .host(values.get(InstanceConfigurationKey.EMAIL_HOST))
                .username(values.get(InstanceConfigurationKey.EMAIL_HOST_USER))
                .password(values.get(InstanceConfigurationKey.EMAIL_HOST_PASSWORD))
                .port(parseInt(values.get(InstanceConfigurationKey.EMAIL_PORT), 587))
                .from(values.get(InstanceConfigurationKey.EMAIL_FROM))
                .useTls(isOn(values.get(InstanceConfigurationKey.EMAIL_USE_TLS)))
                .useSsl(isOn(values.get(InstanceConfigurationKey.EMAIL_USE_SSL)))
                .version(instance.getConfig().getVersion())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthRuntimeConfigResponse getUserAuthRuntimeConfig() {
        Instance instance = latestInstance();

        Map<InstanceConfigurationKey, String> values = resolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_SIGNUP),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_SMTP),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GOOGLE_ENABLED),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GITHUB_ENABLED),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GITLAB_ENABLED)));

        return UserAuthRuntimeConfigResponse.builder()
                .signupEnabled(isOn(values.get(InstanceConfigurationKey.ENABLE_SIGNUP)))
                .magicLinkEnabled(isOn(values.get(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN)))
                .emailPasswordEnabled(isOn(values.get(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD)))
                .smtpEnabled(isOn(values.get(InstanceConfigurationKey.ENABLE_SMTP)))
                .googleEnabled(isOn(values.get(InstanceConfigurationKey.IS_GOOGLE_ENABLED)))
                .githubEnabled(isOn(values.get(InstanceConfigurationKey.IS_GITHUB_ENABLED)))
                .gitlabEnabled(isOn(values.get(InstanceConfigurationKey.IS_GITLAB_ENABLED)))
                .version(instance.getConfig().getVersion())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthRuntimeSecretConfigResponse getUserAuthRuntimeSecretConfig() {
        Instance instance = latestInstance();

        Map<InstanceConfigurationKey, String> values = resolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GOOGLE_CLIENT_ID),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GITHUB_CLIENT_ID),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GITHUB_CLIENT_SECRET),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GITHUB_APP_NAME),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GITLAB_HOST),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GITLAB_CLIENT_ID),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GITLAB_CLIENT_SECRET)));

        return UserAuthRuntimeSecretConfigResponse.builder()
                .googleClientId(values.get(InstanceConfigurationKey.GOOGLE_CLIENT_ID))
                .googleClientSecret(values.get(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET))
                .githubClientId(values.get(InstanceConfigurationKey.GITHUB_CLIENT_ID))
                .githubClientSecret(values.get(InstanceConfigurationKey.GITHUB_CLIENT_SECRET))
                .githubAppName(values.get(InstanceConfigurationKey.GITHUB_APP_NAME))
                .gitlabHost(values.get(InstanceConfigurationKey.GITLAB_HOST))
                .gitlabClientId(values.get(InstanceConfigurationKey.GITLAB_CLIENT_ID))
                .gitlabClientSecret(values.get(InstanceConfigurationKey.GITLAB_CLIENT_SECRET))
                .version(instance.getConfig().getVersion())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceRuntimeConfigResponse getWorkspaceRuntimeConfig() {
        Instance instance = latestInstance();

        Map<InstanceConfigurationKey, String> values = resolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION)));

        return WorkspaceRuntimeConfigResponse.builder()
                .workspaceCreationDisabled(isOn(values.get(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION)))
                .version(instance.getConfig().getVersion())
                .build();
    }

    private Instance latestInstance() {
        return instanceRepository.findTopByOrderByCreatedAtDesc(Instance.class).orElseThrow();
    }

    private boolean isOn(String value) {
        return value != null && !value.isBlank() && !"0".equals(value);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

}
