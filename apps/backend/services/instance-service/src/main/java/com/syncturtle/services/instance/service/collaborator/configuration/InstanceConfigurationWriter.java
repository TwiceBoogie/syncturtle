package com.syncturtle.services.instance.service.collaborator.configuration;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.model.InstanceConfiguration;
import com.syncturtle.services.instance.repository.InstanceConfigurationRepository;
import com.syncturtle.services.instance.service.param.DerivedFlagParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstanceConfigurationWriter {

    private final InstanceConfigurationRepository repo;
    private final InstanceConfigurationPolicy policy;
    private final InstanceConfigurationCrypto crypto;
    private final InstanceConfigurationPropertySource valueSource;

    @Transactional
    public int seedManagedKeysIfMissing() {
        Set<InstanceConfigurationKey> managedKeys = policy.managedKeys();
        List<InstanceConfiguration> existing = repo.findByKeyIn(managedKeys);

        Set<InstanceConfigurationKey> existingKeys = existing.stream()
                .map(InstanceConfiguration::getKey)
                .collect(Collectors.toSet());

        List<InstanceConfiguration> rowsToInsert = managedKeys.stream()
                .filter(key -> !existingKeys.contains(key))
                .map(this::buildManagedRowForInsert)
                .toList();

        if (rowsToInsert.isEmpty()) {
            log.info("All managed config keys already present; nothing to seed.");
            return 0;
        }

        repo.saveAll(rowsToInsert);
        repo.flush();

        log.info("Seeded {} managed instance configuration keys.", rowsToInsert.size());

        return rowsToInsert.size();
    }

    @Transactional
    public boolean ensureDerivedFlagIfMissing(DerivedFlagParam param) {
        Assert.notNull(param, "derived flag param is required");

        policy.requireDerivedKey(param.getKey());

        if (repo.existsByKey(param.getKey())) {
            return false;
        }

        String storedValue = param.isEnabled() ? "1" : "0";
        InstanceConfiguration row = InstanceConfiguration.create(
                param.getKey(),
                storedValue,
                policy.categoryFor(param.getKey()),
                false);
        repo.saveAndFlush(row);

        log.info("Seeded derived instance configuration flag {}={}.", param.getKey(), storedValue);

        return true;
    }

    @Transactional
    public Set<InstanceConfigurationKey> replacePlainValues(Map<InstanceConfigurationKey, String> plainValues) {
        if (plainValues == null || plainValues.isEmpty()) {
            return Set.of();
        }

        List<InstanceConfiguration> rows = repo.findByKeyIn(plainValues.keySet());

        EnumSet<InstanceConfigurationKey> changedKeys = EnumSet.noneOf(InstanceConfigurationKey.class);

        for (InstanceConfiguration row : rows) {
            InstanceConfigurationKey key = row.getKey();
            String nextPlainValue = plainValues.get(key);
            String currentPlainValue = crypto.decryptIfNeeded(row);

            if (currentPlainValue.equals(normalize(nextPlainValue))) {
                continue;
            }

            String nextStoredValue = crypto.encryptIfNeeded(row.isEncrypted(), nextPlainValue);
            if (row.replaceStoredValue(nextStoredValue)) {
                changedKeys.add(key);
            }
        }

        if (!changedKeys.isEmpty()) {
            repo.saveAll(rows);
            repo.flush();
        }

        return changedKeys;
    }

    private InstanceConfiguration buildManagedRowForInsert(InstanceConfigurationKey key) {
        policy.requireManagedKey(key);

        String rawValue = valueSource.getRaw(key);
        if (!StringUtils.hasText(rawValue)) {
            rawValue = policy.defaultFor(key);
        }

        boolean encrypted = policy.isEncrypted(key);
        String storedValue = crypto.encryptIfNeeded(encrypted, rawValue);

        return InstanceConfiguration.create(key, storedValue, policy.categoryFor(key), encrypted);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

}
