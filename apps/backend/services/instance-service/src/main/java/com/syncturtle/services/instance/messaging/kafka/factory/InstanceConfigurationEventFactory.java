package com.syncturtle.services.instance.messaging.kafka.factory;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.instance.model.Instance;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceConfigurationEventFactory {

    private final Clock clock;

    public InstanceConfigurationEvent event(Instance instance, Set<InstanceConfigurationKey> changedKeys,
            InstanceConfigurationScope scope, String correlationId) {
        Assert.notNull(instance, "instance is required");
        Assert.notNull(instance.getId(), "instance id is required");
        Assert.notEmpty(changedKeys, "changedKeys must not be null or empty");
        Assert.hasText(correlationId, "correlationId is required");
        Assert.isTrue(changedKeys.stream().allMatch(key -> key.belongsTo(scope)),
                "every changed key must belong to scope");

        return InstanceConfigurationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .occurredAt(Instant.now(clock))
                .instanceId(instance.getId())
                .scope(scope)
                .configurationVersion(instance.getConfig().getVersion())
                .changedKeys(changedKeys.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()))
                .build();
    }

}
