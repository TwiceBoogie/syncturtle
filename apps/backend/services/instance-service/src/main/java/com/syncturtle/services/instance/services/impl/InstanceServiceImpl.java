package com.syncturtle.services.instance.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.contracts.instance.event.InstanceEvent.Type;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.instance.dto.request.InstanceRequest;
import com.syncturtle.services.instance.models.Instance;
import com.syncturtle.services.instance.models.User;
import com.syncturtle.services.instance.payload.InstanceEventToPublish;
import com.syncturtle.services.instance.payload.InstanceSummary;
import com.syncturtle.services.instance.payload.InstanceSummaryResult;
import com.syncturtle.services.instance.payload.InstanceSummaryWithConfig;
import com.syncturtle.services.instance.payload.InstanceSummaryWithConfigResult;
import com.syncturtle.services.instance.repositories.InstanceRepository;
import com.syncturtle.services.instance.repositories.UserRepository;
import com.syncturtle.services.instance.services.InstanceService;
import com.syncturtle.services.instance.services.configuration.InstanceConfigurationResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceServiceImpl implements InstanceService {

    // repositories
    private final InstanceRepository instanceRepository;
    private final UserRepository userRepository;
    // messenger
    private final ApplicationEventPublisher events;
    // helpers
    private final InstanceConfigurationResolver resolver;
    // context
    private final RequestUserContext userContext;

    @Override
    @Transactional(readOnly = true)
    public Optional<InstanceSummaryWithConfig> instanceInfoAndConfig() {
        // 1: grab instance info
        Instance instance = instanceRepository.findTopByOrderByCreatedAtDesc(Instance.class).orElse(null);

        if (instance == null) {
            return Optional.empty();
        }
        // 2: grab instance configurations
        Map<InstanceConfigurationKey, String> config = resolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_SIGNUP, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GOOGLE_ENABLED, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GITHUB_ENABLED, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GITHUB_APP_NAME, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GITLAB_ENABLED, "0"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN, "1"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD, "1"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.POSTHOG_API_KEY, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.POSTHOG_HOST, ""),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_INTERCOM_ENABLED, "1"),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.INTERCOM_APP_ID, "")));

        // TODO: workspacesLiteRepository.count() >= 1 + usersLiteRepository.count()
        boolean workspacesExist = false;
        long userCount = userRepository.count();

        return Optional.of(new InstanceSummaryWithConfigResult(instance, config, workspacesExist, userCount));
    }

    @Override
    @Transactional
    public InstanceSummary instanceUpdate(InstanceRequest request) {
        Instance instance = instanceRepository.findFirstByOrderByCreatedAtDesc().orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Instance is not registered yet."));

        if (request.getInstanceName() != null) {
            instance.setInstanceName(request.getInstanceName());
        }
        if (request.getTelemetryEnabled() != null) {
            instance.setTelemetryEnabled(request.getTelemetryEnabled());
        }
        instance = instanceRepository.saveAndFlush(instance);

        InstanceEvent event = InstanceEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .type(Type.INSTANCE_UPDATED)
                .id(instance.getId())
                .setupDone(instance.isSetupDone())
                .edition(instance.getEdition())
                .version(instance.getVersion())
                .test(instance.isTest())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();

        events.publishEvent(new InstanceEventToPublish(event));

        boolean workspacesExist = false;
        long userCount = userRepository.count();

        return new InstanceSummaryResult(instance, workspacesExist, userCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getInstanceAdminUserMe() {
        return userRepository.findById(userContext.getUserId());
    }

}
