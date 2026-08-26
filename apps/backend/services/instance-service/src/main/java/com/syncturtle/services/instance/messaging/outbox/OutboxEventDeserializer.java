package com.syncturtle.services.instance.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.messaging.OutboxEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OutboxEventDeserializer {

    private final JsonMapper jsonMapper;

    public OutboxEvent toEvent(OutboxEnvelope envelope) {
        Assert.notNull(envelope, "outbox envelope is required");

        try {
            return jsonMapper.readValue(envelope.getPayload(), payloadType(envelope));
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not deserialize outbox event. outboxId=%s topic=%s eventType=%s"
                            .formatted(envelope.getId(), envelope.getTopic(), envelope.getEventType()),
                    exception);
        }
    }

    private static Class<? extends OutboxEvent> payloadType(OutboxEnvelope envelope) {
        return switch (envelope.getTopic()) {
            case KafkaTopics.INSTANCE_EVENTS_V1 -> InstanceEvent.class;
            case KafkaTopics.INSTANCE_CONFIG_EVENTS_V1 -> InstanceConfigurationEvent.class;
            default ->
                throw new IllegalStateException("Unsupported instance outbox topic. topic=%s eventType=%s outboxId=%s"
                        .formatted(envelope.getTopic(), envelope.getEventType(), envelope.getId()));
        };
    }

}
