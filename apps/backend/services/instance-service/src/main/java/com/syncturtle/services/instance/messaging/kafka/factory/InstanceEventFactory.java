package com.syncturtle.services.instance.messaging.kafka.factory;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.contracts.instance.event.InstanceEvent.Type;
import com.syncturtle.services.instance.model.Instance;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceEventFactory {

    private final Clock clock;

    public InstanceEvent created(Instance instance) {
        return event(Type.INSTANCE_CREATED, instance);
    }

    public InstanceEvent updated(Instance instance) {
        return event(Type.INSTANCE_UPDATED, instance);
    }

    public InstanceEvent softDeleted(Instance instance) {
        return event(Type.INSTANCE_SOFT_DELETED, instance);
    }

    private InstanceEvent event(Type type, Instance instance) {
        Assert.notNull(type, "instance event type is required");
        Assert.notNull(instance, "instance is required");
        Assert.notNull(instance.getId(), "instance id is required");

        Instant now = Instant.now(clock);

        return InstanceEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(now)
                .type(type)
                .id(instance.getId())
                .edition(instance.getEdition())
                .setupDone(instance.isSetupDone())
                .version(instance.getVersion())
                .test(instance.isTest())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();
    }

}
