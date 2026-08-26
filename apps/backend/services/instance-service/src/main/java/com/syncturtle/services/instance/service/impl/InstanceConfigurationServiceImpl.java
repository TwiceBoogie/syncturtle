package com.syncturtle.services.instance.service.impl;

import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.services.instance.messaging.kafka.factory.InstanceConfigurationEventFactory;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceConfiguration;
import com.syncturtle.services.instance.repository.InstanceConfigurationRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceConfigurationService;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationCrypto;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationScopeResolver;
import com.syncturtle.services.instance.service.collaborator.outbox.InstanceOutboxWriter;
import com.syncturtle.services.instance.service.param.RequestedKeyParam;
import com.syncturtle.services.instance.service.result.InstanceConfigResult;

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
    private final InstanceOutboxWriter outboxWriter;
    private final InstanceConfigurationEventFactory eventFactory;
    private final InstanceConfigurationCrypto crypto;
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
        Set<InstanceConfigurationKey> changedKeys = applyPlainValues(configurations, request);

        if (changedKeys.isEmpty()) {
            return List.of();
        }

        commitRevisionAndEvents(instance, configurations, changedKeys);

        return configurations.stream()
                .filter(configuration -> changedKeys.contains(configuration.getKey()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void disableEmail() {
        Instance instance = requireConfiguredInstance();
        List<InstanceConfiguration> configurations = configurationRepository.findByKeyIn(EMAIL_DISABLE_KEYS);
        Map<InstanceConfigurationKey, String> replacements = configurations.stream()
                .collect(Collectors.toMap(InstanceConfiguration::getKey,
                        configuration -> configuration.getKey() == InstanceConfigurationKey.ENABLE_SMTP ? "0" : ""));
        Set<InstanceConfigurationKey> changedKeys = applyPlainValues(configurations, replacements);

        if (!changedKeys.isEmpty()) {
            commitRevisionAndEvents(instance, configurations, changedKeys);
        }
    }

    private Instance requireConfiguredInstance() {
        return instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class).orElseThrow();
    }

    private Set<InstanceConfigurationKey> applyPlainValues(List<InstanceConfiguration> configurations,
            Map<InstanceConfigurationKey, String> values) {
        EnumSet<InstanceConfigurationKey> changedKeys = EnumSet.noneOf(InstanceConfigurationKey.class);

        for (InstanceConfiguration configuration : configurations) {
            String nextPlain = values.get(configuration.getKey());
            // check if decrypted value is equal to new value, if so continue
            if (Objects.equals(crypto.decryptIfNeeded(configuration), nextPlain)) {
                continue;
            }

            String nextStored = crypto.encryptIfNeeded(configuration.isEncrypted(), nextPlain);
            if (configuration.replaceStoredValue(nextStored)) {
                changedKeys.add(configuration.getKey());
            }
        }

        return changedKeys;
    }

    private void commitRevisionAndEvents(Instance instance, List<InstanceConfiguration> configurations,
            Set<InstanceConfigurationKey> changedKeys) {
        instance.bumpConfigVersion(clock);
        instanceRepository.saveAndFlush(instance);
        configurationRepository.saveAll(configurations);

        String correlationId = UUID.randomUUID().toString();
        for (InstanceConfigurationScope scope : InstanceConfigurationScopeResolver.scopesOf(changedKeys)) {
            Set<InstanceConfigurationKey> scopedKeys = InstanceConfigurationScopeResolver.keysInScope(changedKeys,
                    scope);
            InstanceConfigurationEvent event = eventFactory.event(instance, scopedKeys, scope, correlationId);

            outboxWriter.saveInstanceConfigurationEvent(event);
        }
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

}
