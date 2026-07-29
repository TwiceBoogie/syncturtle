package com.syncturtle.services.instance.service.collaborator.outbox;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.messaging.OutboxEvent;
import com.syncturtle.services.instance.model.OutboxMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstanceOutboxWriter {

    private static final String INSTANCE_AGGREGATE_TYPE = "Instance";
    private static final String INSTANCE_CONFIGURATION_AGGREGATE_TYPE = "InstanceConfiguration";

    private final OutboxMessageWriter writer;

    public OutboxMessage saveInstanceEvent(InstanceEvent event) {
        Assert.notNull(event, "instance event is required");
        return save(event, event.getId().toString(), INSTANCE_AGGREGATE_TYPE, event.getId(),
                KafkaTopics.INSTANCE_EVENTS_V1);
    }

    public OutboxMessage saveInstanceConfigurationEvent(InstanceConfigurationEvent event) {
        Assert.notNull(event, "instance configuration event is required");
        return save(event, event.getScope().name(), INSTANCE_CONFIGURATION_AGGREGATE_TYPE, event.getInstanceId(),
                KafkaTopics.INSTANCE_CONFIG_EVENTS_V1);
    }

    private OutboxMessage save(OutboxEvent event, String messageKey, String aggregateType, UUID aggregateId,
            String kafkaTopic) {
        Assert.notNull(event, "instance outbox event is required");
        Assert.hasText(messageKey, "message key is required");
        Assert.hasText(aggregateType, "aggregate type is required");
        Assert.hasText(kafkaTopic, "kafkaTopic is required");

        return writer.save(event, kafkaTopic, messageKey, aggregateType, aggregateId);
    }

}
