package com.syncturtle.platform.services.instance.services.impl;

import java.time.Instant;
import java.util.ArrayList;
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

import com.syncturtle.common.core.enums.InstanceConfigScope;
import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.core.enums.InstanceConfigurationScopes;
import com.syncturtle.common.core.events.InstanceConfigurationEvent;
import com.syncturtle.common.web.dto.response.InstanceConfigResponse;
import com.syncturtle.platform.services.instance.controllers.mappers.InstanceConfigurationApiMapper;
import com.syncturtle.platform.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.platform.services.instance.messaging.db.InstanceConfigurationEventToPublish;
import com.syncturtle.platform.services.instance.models.Instance;
import com.syncturtle.platform.services.instance.models.InstanceConfiguration;
import com.syncturtle.platform.services.instance.repositories.InstanceConfigurationRepository;
import com.syncturtle.platform.services.instance.repositories.InstanceRepository;
import com.syncturtle.platform.services.instance.services.InstanceConfigurationService;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationCrypto;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationResolver;

import lombok.RequiredArgsConstructor;

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

    // repositories
    private final InstanceConfigurationRepository iConfigurationRepository;
    private final InstanceRepository instanceRepository;
    private final InstanceConfigurationResolver instanceConfigurationResolver;
    private final InstanceConfigurationCrypto crypto;
    private final ApplicationEventPublisher publisher;
    private final InstanceConfigurationApiMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public InstanceConfigResponse configurations() {
        Instance instance = instanceRepository.findTopByOrderByCreatedAtDesc(Instance.class).orElseThrow();

        Map<InstanceConfigurationKey, String> configs = instanceConfigurationResolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_SMTP),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GOOGLE_ENABLED),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GITHUB_ENABLED),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_GITLAB_ENABLED),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.GITHUB_APP_NAME),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.IS_INTERCOM_ENABLED)));

        return mapper.toInstanceConfigResponse(configs, instance.getConfig().getVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceConfigurationResponse> configurationsAll() {
        List<InstanceConfiguration> all = iConfigurationRepository.findAll();
        List<InstanceConfigurationResponse> result = new ArrayList<>();
        for (InstanceConfiguration instanceConfiguration : all) {
            result.add(InstanceConfigurationResponse.builder()
                    .id(instanceConfiguration.getId())
                    .key(instanceConfiguration.getKey()).value(crypto.decryptIfNeeded(instanceConfiguration))
                    .createdAt(instanceConfiguration.getCreatedAt()).updatedAt(instanceConfiguration.getUpdatedAt())
                    .createdById(instanceConfiguration.getCreatedById())
                    .updatedById(instanceConfiguration.getUpdatedById()).build());
        }
        return result;
    }

    @Override
    @Transactional
    public List<InstanceConfigurationResponse> configurationsUpdate(Map<InstanceConfigurationKey, String> request) {
        if (request == null || request.isEmpty()) {
            return List.of();
        }

        Instance instance = instanceRepository.findTopByOrderByCreatedAtDesc(Instance.class).orElseThrow();
        List<InstanceConfiguration> configurations = iConfigurationRepository.findByKeyIn(request.keySet());

        Set<InstanceConfigurationKey> changedKeys = EnumSet.noneOf(InstanceConfigurationKey.class);

        for (InstanceConfiguration configuration : configurations) {
            if (!request.containsKey(configuration.getKey())) {
                continue;
            }

            String oldPlainValue = crypto.decryptIfNeeded(configuration);
            String newPlainValue = request.get(configuration.getKey());

            if (Objects.equals(oldPlainValue, newPlainValue)) {
                continue;
            }

            configuration.setValue(crypto.encryptIfNeeded(configuration.isEncrypted(), newPlainValue));
            changedKeys.add(configuration.getKey());
        }

        if (changedKeys.isEmpty()) {
            return List.of();
        }

        instance.getConfig().updateVersion();
        instance = instanceRepository.saveAndFlush(instance);
        configurations = iConfigurationRepository.saveAll(configurations);

        List<InstanceConfigurationResponse> result = configurations.stream()
                .map(row -> InstanceConfigurationResponse.builder()
                        .id(row.getId())
                        .key(row.getKey())
                        .value(crypto.decryptIfNeeded(row))
                        .createdAt(row.getCreatedAt())
                        .updatedAt(row.getUpdatedAt())
                        .createdById(row.getCreatedById())
                        .updatedById(row.getUpdatedById())
                        .build())
                .toList();

        publishEvents(instance, changedKeys);

        return result;
    }

    @Override
    @Transactional
    public void disableEmail() {
        Instance instance = instanceRepository.findTopByOrderByCreatedAtDesc(Instance.class).orElseThrow();
        List<InstanceConfiguration> configurations = iConfigurationRepository.findByKeyIn(EMAIL_DISABLE_KEYS);

        Set<InstanceConfigurationKey> changedKeys = EnumSet.noneOf(InstanceConfigurationKey.class);

        for (InstanceConfiguration configuration : configurations) {
            String nextValue = configuration.getKey() == InstanceConfigurationKey.ENABLE_SMTP ? "0" : "";
            String oldPlainValue = crypto.decryptIfNeeded(configuration);

            if (Objects.equals(oldPlainValue, nextValue)) {
                continue;
            }

            configuration.setValue(crypto.encryptIfNeeded(configuration.isEncrypted(), nextValue));
            changedKeys.add(configuration.getKey());
        }

        if (changedKeys.isEmpty()) {
            return;
        }

        instance.getConfig().updateVersion();
        instance = instanceRepository.saveAndFlush(instance);
        iConfigurationRepository.saveAll(configurations);

        publishEvents(instance, changedKeys);
    }

    private void publishEvents(Instance instance, Set<InstanceConfigurationKey> changedKeys) {
        Instant now = Instant.now();
        String correlationId = UUID.randomUUID().toString();

        EnumSet<InstanceConfigScope> changedScopes = InstanceConfigurationScopes.scopesOf(changedKeys);

        for (InstanceConfigScope scope : changedScopes) {
            Set<InstanceConfigurationKey> scopedKeys = changedKeys.stream()
                    .filter(key -> InstanceConfigurationScopes.scopeOf(key) == scope)
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

    @Override
    @Transactional
    public void disableEmail() {
        Instance instance = instanceRepository.findTopByOrderByCreatedAtDesc(Instance.class).orElseThrow();
        List<InstanceConfiguration> configurations = iConfigurationRepository.findByKeyIn(EMAIL_KEYS);

        for (InstanceConfiguration configuration : configurations) {
            String nextValue = configuration.getKey() == InstanceConfigurationKey.ENABLE_SMTP ? "0" : "";

            configuration.setValue(crypto.encryptIfNeeded(configuration.isEncrypted(), nextValue));
        }

        instance.getConfig().updateVersion();
        instanceRepository.save(instance);
        iConfigurationRepository.saveAll(configurations);
    }

}
