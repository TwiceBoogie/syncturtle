package com.syncturtle.common.contracts.instance.event;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScopeNames;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InstanceConfigurationEvent {
    String eventId;
    String correlationId;
    Instant occurredAt;
    UUID instanceId;
    InstanceConfigurationScopeNames scope;
    Long scopeVersion;
    Long globalVersion;
    @Builder.Default
    private Set<InstanceConfigurationKey> changedKeys = Collections.emptySet();
}
