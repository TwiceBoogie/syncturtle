package com.syncturtle.common.contracts.instance.event;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScopeNames;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Jacksonized
@Builder(toBuilder = true)
public final class InstanceConfigurationEvent {
    private final String eventId;
    private final String correlationId;
    private final Instant occurredAt;
    private final UUID instanceId;
    private final InstanceConfigurationScopeNames scope;
    private final Long scopeVersion;
    private final Long globalVersion;
    private final Set<InstanceConfigurationKey> changedKeys;

    private InstanceConfigurationEvent(
            String eventId,
            String correlationId,
            Instant occurredAt,
            UUID instanceId,
            InstanceConfigurationScopeNames scope,
            Long scopeVersion,
            Long globalVersion,
            Set<InstanceConfigurationKey> changedKeys) {
        this.eventId = requireText(eventId, "eventId is required");
        this.correlationId = normalizeNullable(correlationId);
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId is required");
        this.scope = Objects.requireNonNull(scope, "scope is required");
        this.scopeVersion = scopeVersion;
        this.globalVersion = Objects.requireNonNull(globalVersion, "globalVersion is required");
        this.changedKeys = immutableChangedKeys(changedKeys);
    }

    private static Set<InstanceConfigurationKey> immutableChangedKeys(Set<InstanceConfigurationKey> changedKeys) {
        if (changedKeys == null || changedKeys.isEmpty()) {
            return Collections.emptySet();
        }

        return Set.copyOf(changedKeys);
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
