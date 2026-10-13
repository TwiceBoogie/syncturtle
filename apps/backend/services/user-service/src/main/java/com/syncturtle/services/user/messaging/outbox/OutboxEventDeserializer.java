package com.syncturtle.services.user.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

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
            OutboxEvent event = jsonMapper.readValue(envelope.getPayload(), UserOutboxEventMapping.payloadType(envelope));
            if (!envelope.getEventType().equals(event.eventTypeName())) {
                throw new IllegalStateException("Outbox payload event type does not match the stored event type. "
                        + "outboxId=%s topic=%s eventType=%s payloadEventType=%s"
                                .formatted(envelope.getId(), envelope.getTopic(), envelope.getEventType(),
                                        event.eventTypeName()));
            }
            return event;
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not deserialize outbox event. outboxId=%s topic=%s eventType=%s"
                            .formatted(envelope.getId(), envelope.getTopic(), envelope.getEventType()),
                    exception);
        }
    }

}
