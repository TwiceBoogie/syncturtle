package com.syncturtle.services.instance.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScopeNames;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.services.instance.dto.result.InstanceConfigResult;
import com.syncturtle.services.instance.messaging.db.event.InstanceConfigurationEventToPublish;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceConfiguration;
import com.syncturtle.services.instance.repository.InstanceConfigurationRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceConfigurationService;
import com.syncturtle.services.instance.service.configuration.InstanceConfigurationCrypto;
import com.syncturtle.services.instance.service.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.configuration.param.RequestedKeyParam;
import com.syncturtle.services.instance.util.InstanceConfigurationScope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceConfigurationServiceImpl implements InstanceConfigurationService {

    private static final Set<InstanceConfigurationKey> EMAIL_DISABLE_KEYS = Set.of(
            InstanceConfigurationKey.EMAIL_HOST,
            InstanceConfigurationKey.EMAIL_HOST_USER,
            InstanceConfigurationKey.EMAIL_HOST_PASSWORD,
            InstanceConfigurationKey.ENABLE_SMTP,
            InstanceConfigurationKey.EMAIL_PORT,
            InstanceConfigurationKey.EMAIL_FROM,
            InstanceConfigurationKey.EMAIL_USE_TLS,
            InstanceConfigurationKey.EMAIL_USE_SSL);

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

    // repositories
    private final InstanceConfigurationRepository configurationRepository;
    private final InstanceRepository instanceRepository;
    private final InstanceConfigurationResolver configurationResolver;
    private final InstanceConfigurationCrypto crypto;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public InstanceConfigResult configurations() {
        Instance instance = requireConfiguredInstance();

        Map<InstanceConfigurationKey, String> configs = configurationResolver
                .resolveRequested(INSTANCE_CONFIGURATION_KEYS);

        return InstanceConfigResult.builder()
                .values(configs)
                .version(instance.getConfig().getVersion())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceConfigurationResponse> configurationsAll() {
        return configurationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<InstanceConfigurationResponse> configurationsUpdate(Map<InstanceConfigurationKey, String> request) {
        if (request == null || request.isEmpty()) {
            return List.of();
        }

        Instance instance = requireConfiguredInstance();

        List<InstanceConfiguration> configurations = configurationRepository.findByKeyIn(request.keySet());

        Set<InstanceConfigurationKey> changedKeys = EnumSet.noneOf(InstanceConfigurationKey.class);
        for (InstanceConfiguration configuration : configurations) {
            String newPlainValue = request.get(configuration.getKey());
            String oldPlainValue = crypto.decryptIfNeeded(configuration);

            if (Objects.equals(oldPlainValue, newPlainValue)) {
                continue;
            }

            String nextStoredValue = crypto.encryptIfNeeded(configuration.isEncrypted(), newPlainValue);

            boolean changed = configuration.replaceStoredValue(nextStoredValue);
            if (changed) {
                changedKeys.add(configuration.getKey());
            }
        }
        log.info("changedKeys: {}", changedKeys);
        if (changedKeys.isEmpty()) {
            return List.of();
        }

        instance.bumpConfigVersion(clock);
        instanceRepository.saveAndFlush(instance);

        List<InstanceConfiguration> savedConfigurations = configurationRepository.saveAll(configurations);

        publishEvents(instance, changedKeys);

        return savedConfigurations.stream()
                .filter(configuration -> changedKeys.contains(configuration.getKey()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void disableEmail() {
        Instance instance = requireConfiguredInstance();

        List<InstanceConfiguration> configurations = configurationRepository.findByKeyIn(EMAIL_DISABLE_KEYS);

        Set<InstanceConfigurationKey> changedKeys = EnumSet.noneOf(InstanceConfigurationKey.class);

        for (InstanceConfiguration configuration : configurations) {
            String nextPlainValue = configuration.getKey() == InstanceConfigurationKey.ENABLE_SMTP ? "0" : "";
            String oldPlainValue = crypto.decryptIfNeeded(configuration);

            if (Objects.equals(oldPlainValue, nextPlainValue)) {
                continue;
            }

            String nextStoredValue = crypto.encryptIfNeeded(configuration.isEncrypted(), nextPlainValue);
            boolean changed = configuration.replaceStoredValue(nextStoredValue);
            if (changed) {
                changedKeys.add(configuration.getKey());
            }
        }

        if (changedKeys.isEmpty()) {
            return;
        }

        instance.bumpConfigVersion(clock);
        instanceRepository.saveAndFlush(instance);
        configurationRepository.saveAll(configurations);

        publishEvents(instance, changedKeys);
    }

    private Instance requireConfiguredInstance() {
        return instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class).orElseThrow();
    }

    private InstanceConfigurationResponse toResponse(InstanceConfiguration configuration) {
        return InstanceConfigurationResponse.builder()
                .id(configuration.getId())
                .key(configuration.getKey())
                .value(crypto.decryptIfNeeded(configuration))
                .createdAt(configuration.getCreatedAt())
                .updatedAt(configuration.getUpdatedAt())
                .createdById(configuration.getCreatedById())
                .updatedById(configuration.getUpdatedById())
                .build();
    }

    private void publishEvents(Instance instance, Set<InstanceConfigurationKey> changedKeys) {
        Instant now = Instant.now(clock);
        String correlationId = UUID.randomUUID().toString();

        EnumSet<InstanceConfigurationScopeNames> changedScopes = InstanceConfigurationScope.scopesOf(changedKeys);

        for (InstanceConfigurationScopeNames scope : changedScopes) {
            Set<InstanceConfigurationKey> scopedKeys = changedKeys.stream()
                    .filter(key -> InstanceConfigurationScope.scopeOf(key) == scope)
                    .collect(Collectors.toSet());

            InstanceConfigurationEvent event = InstanceConfigurationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .correlationId(correlationId)
                    .occurredAt(now)
                    .instanceId(instance.getId())
                    .scope(scope)
                    .changedKeys(scopedKeys)
                    .globalVersion(instance.getConfig().getVersion())
                    .build();

            publisher.publishEvent(new InstanceConfigurationEventToPublish(event));
        }
    }

}
