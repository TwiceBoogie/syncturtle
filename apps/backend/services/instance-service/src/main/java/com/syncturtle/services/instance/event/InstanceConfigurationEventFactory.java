package com.syncturtle.services.instance.event;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScopeNames;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.instance.model.Instance;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceConfigurationEventFactory {

    private final Clock clock;

    public InstanceConfigurationEvent event(Instance instance, Set<InstanceConfigurationKey> changedKeys,
            InstanceConfigurationScopeNames scope) {
        Assert.notNull(instance, "instance is required");
        Assert.notNull(instance.getId(), "instance id is required");
        Assert.notEmpty(changedKeys, "changedKeys must not be null or empty");

        Instant now = Instant.now(clock);

        return InstanceConfigurationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(instance.getId().toString())
                .occurredAt(now)
                .instanceId(instance.getId())
                .scope(scope)
                .changedKeys(changedKeys)
                .scopeVersion(instance.getConfig().getVersion())
                .globalVersion(instance.getConfig().getVersion())
                .build();
    }

}
