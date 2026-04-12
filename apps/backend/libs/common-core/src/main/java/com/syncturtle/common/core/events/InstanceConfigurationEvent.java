package com.syncturtle.common.core.events;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

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
    private String correlationId;
    public Instant occurredAt;

    private UUID instanceId;

    private InstanceConfigScope scope;
    private Long scopeVersion;
    private Long globalVersion;

    @Builder.Default
    private Set<InstanceConfigurationKey> changedKeys = Collections.emptySet();

}
