package com.syncturtle.services.instance.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.web.property.PublicUrlProperties;
import com.syncturtle.services.instance.dto.request.InstanceUpdateRequest;
import com.syncturtle.services.instance.dto.response.InstanceResponse;
import com.syncturtle.services.instance.dto.response.InstanceSetupResponse;
import com.syncturtle.services.instance.mapper.InstanceApiMapper;
import com.syncturtle.services.instance.mapper.InstanceConfigurationApiMapper;
import com.syncturtle.services.instance.messaging.kafka.factory.InstanceEventFactory;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.UserRepository;
import com.syncturtle.services.instance.repository.WorkspaceRepository;
import com.syncturtle.services.instance.service.InstanceService;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.collaborator.outbox.InstanceOutboxWriter;
import com.syncturtle.services.instance.service.param.RequestedKeyParam;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceServiceImpl implements InstanceService {

    private static final List<RequestedKeyParam> INSTANCE_CONFIGURATION_KEYS = List.of(
            RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_SIGNUP),
            RequestedKeyParam.of(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION),
            RequestedKeyParam.of(InstanceConfigurationKey.IS_GOOGLE_ENABLED),
            RequestedKeyParam.of(InstanceConfigurationKey.IS_GITHUB_ENABLED),
            RequestedKeyParam.of(InstanceConfigurationKey.GITHUB_APP_NAME),
            RequestedKeyParam.of(InstanceConfigurationKey.IS_GITLAB_ENABLED),
            RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_HOST),
            RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN),
            RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD),
            RequestedKeyParam.of(InstanceConfigurationKey.POSTHOG_API_KEY),
            RequestedKeyParam.of(InstanceConfigurationKey.POSTHOG_HOST),
            RequestedKeyParam.of(InstanceConfigurationKey.IS_INTERCOM_ENABLED),
            RequestedKeyParam.of(InstanceConfigurationKey.INTERCOM_APP_ID));

    private final InstanceRepository instanceRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final InstanceOutboxWriter outboxWriter;
    private final InstanceEventFactory eventFactory;
    private final InstanceConfigurationResolver resolver;
    private final InstanceApiMapper instanceApiMapper;
    private final InstanceConfigurationApiMapper configurationApiMapper;
    private final PublicUrlProperties properties;

    @Override
    @Transactional(readOnly = true)
    public InstanceSetupResponse getPublicInstance() {
        Instance instance = instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElse(null);

        if (instance == null) {
            return instanceApiMapper.toInactiveInstanceResponse();
        }

        Map<InstanceConfigurationKey, String> config = resolver.resolveRequested(INSTANCE_CONFIGURATION_KEYS);

        boolean workspacesExist = workspaceRepository.countByDeletedAtIsNull() >= 1;
        long userCount = userRepository.countByActiveTrue();

        return InstanceSetupResponse.builder()
                .config(configurationApiMapper.toResponse(config, properties))
                .instance(instanceApiMapper.toInstanceResponse(instance, userCount, workspacesExist))
                .build();
    }

    @Override
    @Transactional
    public InstanceResponse instanceUpdate(InstanceUpdateRequest request) {
        Assert.notNull(request, "request is required");

        Instance instance = requireConfiguredInstance();

        if (request.getInstanceName() != null) {
            instance.rename(request.getInstanceName());
        }

        if (request.isTelemetryEnabled()) {
            instance.enableTelemetry();
        } else {
            instance.disableTelemetry();
        }

        instance = instanceRepository.saveAndFlush(instance);
        publishInstanceUpdate(instance);

        boolean workspacesExist = workspaceRepository.countByDeletedAtIsNull() >= 1;
        long userCount = userRepository.countByActiveTrue();

        return instanceApiMapper.toInstanceResponse(instance, userCount, workspacesExist);
    }

    @Override
    @Transactional
    public void markSignupScreenVisited() {
        Instance instance = requireConfiguredInstance();

        instance.markSignupScreenVisited();
        instance = instanceRepository.saveAndFlush(instance);
        publishInstanceUpdate(instance);
    }

    private Instance requireConfiguredInstance() {
        return instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Instance is not configured."));
    }

    private void publishInstanceUpdate(Instance instance) {
        InstanceEvent event = eventFactory.updated(instance);
        outboxWriter.saveInstanceEvent(event);
    }

}
