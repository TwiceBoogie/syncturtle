package com.syncturtle.services.user.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.user.event.UserOutboxEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxEventDeserializer {

    private final ObjectMapper objectMapper;

    public UserOutboxEvent toUserEvent(OutboxEnvelope envelope) {
        Assert.notNull(envelope, "outbox envelope is required");

        try {
            return objectMapper.readValue(envelope.getPayload(), payloadType(envelope));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not deserialize user outbox event. outboxId=%s topic=%s eventType=%s"
                            .formatted(envelope.getId(), envelope.getTopic(), envelope.getEventType()),
                    exception);
        }
    }

    private static Class<? extends UserOutboxEvent> payloadType(OutboxEnvelope envelope) {
        return switch (envelope.getTopic()) {
            case KafkaTopics.USER_EVENTS_V1 -> UserEvent.class;
            default ->
                throw new IllegalStateException("Unsupported user outbox topic. topic=%s eventType=%s outboxId=%s"
                        .formatted(envelope.getTopic(), envelope.getEventType(), envelope.getId()));
        };
    }

}
