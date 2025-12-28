package com.syncturtle.platform.services.instance.services.configuration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.models.InstanceConfiguration;
import com.syncturtle.platform.services.instance.repositories.InstanceConfigurationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstanceConfigurationWriter {

    private final InstanceConfigurationRepository repo;
    private final InstanceConfigurationPolicy policy;
    private final InstanceConfigurationCrypto crypto;
    private final InstancePropertyValueSource valueSource;

    @Transactional
    public void seedManagedKeysIfMissing() {
        Set<InstanceConfigurationKey> managed = policy.managedKeys();

        List<InstanceConfiguration> existing = repo.findByKeyIn(managed);
        Set<InstanceConfigurationKey> existingKeys = existing.stream()
                .map(InstanceConfiguration::getKey)
                .collect(Collectors.toSet());

        List<InstanceConfiguration> toInsert = managed.stream()
                .filter(k -> !existingKeys.contains(k))
                .map(this::buildRowForInsert)
                .toList();

        if (toInsert.isEmpty()) {
            log.info("All managed config keys already present; nothing to seed.");
            return;
        }

        for (InstanceConfiguration row : toInsert) {
            try {
                repo.save(row);
            } catch (DataIntegrityViolationException e) {
                // race safe; ignore dupes
            }
        }
        log.info("Seeded {} managed config keys (existing untouched).", toInsert.size());
    }

    @Transactional
    public void ensureDerivedFlagIfMissing(InstanceConfigurationKey flagKey, boolean enabled) {
        if (repo.existsByKey(flagKey)) {
            return;
        }

        InstanceConfiguration row = new InstanceConfiguration();
        row.setKey(flagKey);
        row.setCategory(policy.categoryFor(flagKey));
        row.setEncrypted(false);
        row.setValue(enabled ? "1" : "0");

        try {
            repo.save(row);
            log.info("Computed {} = {}", flagKey, enabled ? "1" : "0");
        } catch (DataIntegrityViolationException e) {
            // ignore
        }
    }

    private InstanceConfiguration buildRowForInsert(InstanceConfigurationKey key) {
        String raw = valueSource.getRaw(key);
        if (!StringUtils.hasText(raw)) {
            raw = policy.defaultFor(key);
        }

        boolean encrypted = policy.isEncrypted(key);
        String stored = crypto.encryptIfNeeded(encrypted, raw);

        InstanceConfiguration row = new InstanceConfiguration();
        row.setKey(key);
        row.setCategory(policy.categoryFor(key));
        row.setEncrypted(encrypted);
        row.setValue(stored);
        return row;
    }
}
