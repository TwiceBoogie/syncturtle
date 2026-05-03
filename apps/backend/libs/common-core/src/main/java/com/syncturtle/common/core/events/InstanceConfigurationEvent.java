package com.syncturtle.common.core.events;

import java.time.Instant;
import java.util.Set;

import com.syncturtle.common.core.enums.InstanceConfigScope;
import com.syncturtle.common.core.enums.InstanceConfigurationKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceConfigurationEvent {
    public String eventId;
    public Instant occurredAt;

    private String correlationId;

    private InstanceConfigScope scope;
    private Set<InstanceConfigurationKey> changedKeys;

    private Long scopeVersion;
    private Long globalVersion;
}
