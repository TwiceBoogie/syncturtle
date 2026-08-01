package com.syncturtle.common.contracts.instance.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.messaging.OutboxEvent;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class InstanceConfigurationEvent implements OutboxEvent {

    public static final String EVENT_TYPE = "INSTANCE_CONFIGURATION_CHANGED";

    private final String eventId;
    private final String correlationId;
    private final Instant occurredAt;
    private final UUID instanceId;
    private final InstanceConfigurationScope scope;
    private final long configurationVersion;
    private final Set<String> changedKeys;

    @Builder
    @Jacksonized
    private InstanceConfigurationEvent(
            String eventId,
            String correlationId,
            Instant occurredAt,
            UUID instanceId,
            InstanceConfigurationScope scope,
            Long configurationVersion,
            Set<String> changedKeys) {
        this.eventId = requireText(eventId, "eventId is required");
        this.correlationId = normalizeNullable(correlationId);
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId is required");
        this.scope = Objects.requireNonNull(scope, "scope is required");
        this.configurationVersion = requireVersion(configurationVersion);
        this.changedKeys = immutableAndValidateChangedKeys(changedKeys, scope);
    }

    @Override
    public String eventTypeName() {
        return EVENT_TYPE;
    }

    public boolean invalidatesCompleteScope() {
        return changedKeys.isEmpty() || changedKeys.stream()
                .anyMatch(keyName -> InstanceConfigurationKey.parseOrNull(keyName) == null);
    }

    public boolean affectsAny(Set<InstanceConfigurationKey> relevantKeys) {
        Objects.requireNonNull(relevantKeys, "relevantKeys is required");

        if (invalidatesCompleteScope()) {
            return true;
        }

        return relevantKeys.stream().map(Enum::name).anyMatch(changedKeys::contains);
    }

    private static long requireVersion(Long value) {
        Objects.requireNonNull(value, "configurationVersion is required");
        if (value < 0) {
            throw new IllegalArgumentException("configurationVersion must not be negative");
        }
        return value;
    }

    private static Set<String> immutableAndValidateChangedKeys(Set<String> values,
            InstanceConfigurationScope scope) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String keyName = requireText(value, "changedKeys must not contain blank entries");
            InstanceConfigurationKey knownKey = InstanceConfigurationKey.parseOrNull(keyName);
            if (knownKey != null && !knownKey.belongsTo(scope)) {
                throw new IllegalArgumentException(
                        "Configuration key %s does not belong to scope %s".formatted(keyName, scope));
            }
            normalized.add(keyName);
        }
        return Set.copyOf(normalized);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
