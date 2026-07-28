package com.syncturtle.services.user.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.messaging.OutboxEvent;
import com.syncturtle.common.contracts.user.event.UserEvent;

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
            case KafkaTopics.USER_EVENTS_V1 -> UserEvent.class;
            case KafkaTopics.EMAIL_EVENTS_V1 -> emailPayloadType(envelope);
            default ->
                throw new IllegalStateException("Unsupported user outbox topic. topic=%s eventType=%s outboxId=%s"
                        .formatted(envelope.getTopic(), envelope.getEventType(), envelope.getId()));
        };
    }

    private static Class<? extends OutboxEvent> emailPayloadType(OutboxEnvelope envelope) {
        return switch (envelope.getEventType()) {
            case EmailToSendEvent.EVENT_TYPE -> EmailToSendEvent.class;
            default ->
                throw new IllegalStateException("Unsupported email outbox event type. topic=%s eventType=%s outboxId=%s"
                        .formatted(envelope.getTopic(), envelope.getEventType(), envelope.getId()));
        };
    }

}
