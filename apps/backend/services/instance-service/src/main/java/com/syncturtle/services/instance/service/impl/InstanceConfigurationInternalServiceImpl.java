package com.syncturtle.services.instance.service.impl;

import static com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver.isOn;
import static com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver.parseIntOr;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeConfigResponse;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.workspace.config.WorkspaceRuntimeConfigResponse;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceConfigurationInternalService;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.param.RequestedKeyParam;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceConfigurationInternalServiceImpl implements InstanceConfigurationInternalService {

    private static final List<RequestedKeyParam> EMAIL_SECRET_KEYS = List.of(
            RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_SMTP),
            RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_HOST),
            RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_HOST_USER),
            RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_HOST_PASSWORD),
            RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_PORT),
            RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_FROM),
            RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_USE_TLS),
            RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_USE_SSL));

    private static final List<RequestedKeyParam> USER_AUTH_RUNTIME_KEYS = List.of(
            RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_SIGNUP),
            RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN),
            RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD),
            RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_SMTP),
            RequestedKeyParam.of(InstanceConfigurationKey.IS_GOOGLE_ENABLED),
            RequestedKeyParam.of(InstanceConfigurationKey.IS_GITHUB_ENABLED),
            RequestedKeyParam.of(InstanceConfigurationKey.IS_GITLAB_ENABLED));

    private static final List<RequestedKeyParam> USER_AUTH_SECRET_KEYS = List.of(
            RequestedKeyParam.of(InstanceConfigurationKey.GOOGLE_CLIENT_ID),
            RequestedKeyParam.of(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET),
            RequestedKeyParam.of(InstanceConfigurationKey.GITHUB_CLIENT_ID),
            RequestedKeyParam.of(InstanceConfigurationKey.GITHUB_CLIENT_SECRET),
            RequestedKeyParam.of(InstanceConfigurationKey.GITHUB_APP_NAME),
            RequestedKeyParam.of(InstanceConfigurationKey.GITLAB_HOST),
            RequestedKeyParam.of(InstanceConfigurationKey.GITLAB_CLIENT_ID),
            RequestedKeyParam.of(InstanceConfigurationKey.GITLAB_CLIENT_SECRET));

    private static final List<RequestedKeyParam> WORKSPACE_RUNTIME_KEYS = List.of(
            RequestedKeyParam.of(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION));

    private final InstanceRepository instanceRepository;
    private final InstanceConfigurationResolver configurationResolver;

    @Override
    @Transactional(readOnly = true)
    public EmailRuntimeSecretConfigResponse getEmailRuntimeSecretConfig() {
        Instance instance = latestInstance();

        Map<InstanceConfigurationKey, String> values = configurationResolver
                .resolveRequested(EMAIL_SECRET_KEYS);

        return EmailRuntimeSecretConfigResponse.builder()
                .enabled(isOn(values.get(InstanceConfigurationKey.ENABLE_SMTP)))
                .host(values.get(InstanceConfigurationKey.EMAIL_HOST))
                .username(values.get(InstanceConfigurationKey.EMAIL_HOST_USER))
                .password(values.get(InstanceConfigurationKey.EMAIL_HOST_PASSWORD))
                .port(parseIntOr(values.get(InstanceConfigurationKey.EMAIL_PORT), 587))
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

        Map<InstanceConfigurationKey, String> values = configurationResolver
                .resolveRequested(USER_AUTH_RUNTIME_KEYS);

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

        Map<InstanceConfigurationKey, String> values = configurationResolver
                .resolveRequested(USER_AUTH_SECRET_KEYS);

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

        Map<InstanceConfigurationKey, String> values = configurationResolver
                .resolveRequested(WORKSPACE_RUNTIME_KEYS);

        return WorkspaceRuntimeConfigResponse.builder()
                .workspaceCreationDisabled(
                        isOn(values.get(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION)))
                .version(instance.getConfig().getVersion())
                .build();
    }

    private Instance latestInstance() {
        return instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElseThrow(() -> new IllegalStateException("Instance is not configured"));
    }

}
