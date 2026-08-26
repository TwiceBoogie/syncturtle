package com.syncturtle.services.instance.service.impl;

import java.time.Clock;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.services.instance.bootstrap.InstanceRegistrar;
import com.syncturtle.services.instance.messaging.kafka.factory.InstanceConfigurationEventFactory;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceConfiguration;
import com.syncturtle.services.instance.repository.InstanceConfigurationRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceSetupService;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationCrypto;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationPolicy;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationPropertySource;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationScopeResolver;
import com.syncturtle.services.instance.service.collaborator.outbox.InstanceOutboxWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Profile("setup")
@RequiredArgsConstructor
public class InstanceSetupServiceImpl implements InstanceSetupService {

    private final InstanceRegistrar registrar;
    private final InstanceRepository instanceRepository;
    private final InstanceConfigurationRepository configurationRepository;
    private final InstanceConfigurationPolicy policy;
    private final InstanceConfigurationPropertySource valueSource;
    private final InstanceConfigurationResolver resolver;
    private final InstanceConfigurationCrypto crypto;
    private final InstanceConfigurationEventFactory eventFactory;
    private final InstanceOutboxWriter outboxWriter;
    private final Clock clock;

    @Override
    @Transactional
    public void setup(String machineSignature) {
        resolver.ensureMandatorySecretsPresentOrThrow();
        registrar.run(machineSignature);

        Instance instance = requireInstance();
        EnumSet<InstanceConfigurationKey> setupKeys = setupKeys();
        Map<InstanceConfigurationKey, String> before = resolveEffectiveValues(setupKeys);

        Map<InstanceConfigurationKey, InstanceConfiguration> rowsByKey = loadConfigurationRowsByKey(setupKeys);

        int managedRowsInserted = insertMissingManagedRows(rowsByKey);
        int derivedRowsChanged = reconcileDerivedRows(rowsByKey);

        configurationRepository.saveAll(rowsByKey.values());
        configurationRepository.flush();

        Map<InstanceConfigurationKey, String> after = resolveEffectiveValues(setupKeys);
        EnumSet<InstanceConfigurationKey> changedKeys = findEffectiveChanges(setupKeys, before, after);

        if (!changedKeys.isEmpty()) {
            instance.bumpConfigVersion(clock);
            instanceRepository.saveAndFlush(instance);

            publishEvents(instance, changedKeys);
        }

        log.info(
                "Instance setup configuration committed. managedInserted={} derivedRowsChanged={} effectiveChanges={} configurationVersion={}",
                managedRowsInserted, derivedRowsChanged, changedKeys.size(), instance.getConfig().getVersion());
    }

    private Map<InstanceConfigurationKey, InstanceConfiguration> loadConfigurationRowsByKey(
            Set<InstanceConfigurationKey> keys) {
        List<InstanceConfiguration> existingRows = configurationRepository.findByKeyIn(keys);
        Map<InstanceConfigurationKey, InstanceConfiguration> rowsByKey = new EnumMap<>(InstanceConfigurationKey.class);

        for (InstanceConfiguration row : existingRows) {
            InstanceConfigurationKey key = row.getKey();

            // keep the first row encountered for a key
            if (!rowsByKey.containsKey(key)) {
                rowsByKey.put(key, row);
            }
        }

        return rowsByKey;
    }

    private int insertMissingManagedRows(Map<InstanceConfigurationKey, InstanceConfiguration> rowsByKey) {
        int inserted = 0;

        for (InstanceConfigurationKey key : policy.managedKeys()) {
            if (rowsByKey.containsKey(key)) {
                continue;
            }

            String raw = valueSource.getRaw(key);

            if (!StringUtils.hasText(raw)) {
                raw = policy.defaultFor(key);
            }

            boolean encrypted = policy.isEncrypted(key);
            String stored = crypto.encryptIfNeeded(encrypted, raw);
            InstanceConfiguration configuration = InstanceConfiguration.create(
                    key,
                    stored,
                    policy.categoryFor(key),
                    encrypted);
            rowsByKey.put(key, configuration);
            inserted++;
        }

        return inserted;
    }

    private int reconcileDerivedRows(Map<InstanceConfigurationKey, InstanceConfiguration> rowsByKey) {
        int changed = 0;

        changed += reconcileDerivedRow(
                rowsByKey,
                InstanceConfigurationKey.IS_GOOGLE_ENABLED,
                enabled(hasAll(
                        InstanceConfigurationKey.GOOGLE_CLIENT_ID,
                        InstanceConfigurationKey.GOOGLE_CLIENT_SECRET)));

        changed += reconcileDerivedRow(
                rowsByKey,
                InstanceConfigurationKey.IS_GITHUB_ENABLED,
                enabled(hasAll(
                        InstanceConfigurationKey.GITHUB_CLIENT_ID,
                        InstanceConfigurationKey.GITHUB_CLIENT_SECRET)));

        changed += reconcileDerivedRow(
                rowsByKey,
                InstanceConfigurationKey.IS_GITLAB_ENABLED,
                enabled(hasAll(
                        InstanceConfigurationKey.GITLAB_HOST,
                        InstanceConfigurationKey.GITLAB_CLIENT_ID,
                        InstanceConfigurationKey.GITLAB_CLIENT_SECRET)));

        changed += reconcileDerivedRow(
                rowsByKey,
                InstanceConfigurationKey.IS_INTERCOM_ENABLED,
                enabled(hasAll(
                        InstanceConfigurationKey.INTERCOM_APP_ID)));

        return changed;
    }

    private int reconcileDerivedRow(Map<InstanceConfigurationKey, InstanceConfiguration> rowsByKey,
            InstanceConfigurationKey key, String desiredValue) {
        policy.requireDerivedKey(key);

        InstanceConfiguration existingRow = rowsByKey.get(key);

        if (existingRow == null) {
            InstanceConfiguration newRow = InstanceConfiguration.create(key, desiredValue, policy.categoryFor(key),
                    false);
            rowsByKey.put(key, newRow);
            return 1;
        }

        return existingRow.replaceStoredValue(desiredValue) ? 1 : 0;
    }

    private boolean hasAll(InstanceConfigurationKey... keys) {
        for (InstanceConfigurationKey key : keys) {
            String resolvedValue = resolver.resolveValue(key);

            if (!StringUtils.hasText(resolvedValue)) {
                return false;
            }
        }

        return true;
    }

    private Map<InstanceConfigurationKey, String> resolveEffectiveValues(Set<InstanceConfigurationKey> keys) {
        Map<InstanceConfigurationKey, String> values = new EnumMap<>(InstanceConfigurationKey.class);

        for (InstanceConfigurationKey key : keys) {
            String resolvedValue = resolver.resolveValue(key);
            values.put(key, resolvedValue);
        }

        return values;
    }

    private EnumSet<InstanceConfigurationKey> findEffectiveChanges(
            Set<InstanceConfigurationKey> keys,
            Map<InstanceConfigurationKey, String> before,
            Map<InstanceConfigurationKey, String> after) {
        EnumSet<InstanceConfigurationKey> changedKeys = EnumSet.noneOf(InstanceConfigurationKey.class);

        for (InstanceConfigurationKey key : keys) {
            String prev = before.get(key);
            String current = after.get(key);

            if (!Objects.equals(prev, current)) {
                changedKeys.add(key);
            }
        }

        return changedKeys;
    }

    private void publishEvents(Instance instance, Set<InstanceConfigurationKey> changedKeys) {
        String correlationId = UUID.randomUUID().toString();
        Set<InstanceConfigurationScope> changedScopes = InstanceConfigurationScopeResolver.scopesOf(changedKeys);

        for (InstanceConfigurationScope scope : changedScopes) {
            Set<InstanceConfigurationKey> scopedKeys = InstanceConfigurationScopeResolver
                    .keysInScope(changedKeys, scope);
            outboxWriter.saveInstanceConfigurationEvent(eventFactory.event(instance, scopedKeys, scope, correlationId));
        }
    }

    private Instance requireInstance() {
        return instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElseThrow(() -> new IllegalStateException("Instance registration did not create an instance"));
    }

    private EnumSet<InstanceConfigurationKey> setupKeys() {
        EnumSet<InstanceConfigurationKey> keys = EnumSet.noneOf(InstanceConfigurationKey.class);
        keys.addAll(policy.managedKeys());
        keys.addAll(policy.derivedKeys());
        return keys;
    }

    private static String enabled(boolean value) {
        return value ? "1" : "0";
    }

}
