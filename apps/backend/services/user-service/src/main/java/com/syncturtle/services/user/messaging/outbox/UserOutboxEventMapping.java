package com.syncturtle.services.user.messaging.outbox;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.messaging.OutboxEvent;
import com.syncturtle.common.contracts.user.event.UserAuthenticatedEvent;
import com.syncturtle.common.contracts.user.event.UserEvent;

/** The event/topic pairs that user-service can persist and relay. */
public final class UserOutboxEventMapping {

    private UserOutboxEventMapping() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String topicFor(OutboxEvent event) {
        Assert.notNull(event, "outbox event is required");

        for (SupportedEvent supported : SupportedEvent.values()) {
            if (supported.eventType.equals(event.eventTypeName()) && supported.payloadType.isInstance(event)) {
                return supported.topic;
            }
        }

        throw new IllegalArgumentException("Unsupported user outbox event. eventType=%s payloadType=%s"
                .formatted(event.eventTypeName(), event.getClass().getName()));
    }

    public static Class<? extends OutboxEvent> payloadType(OutboxEnvelope envelope) {
        Assert.notNull(envelope, "outbox envelope is required");

        for (SupportedEvent supported : SupportedEvent.values()) {
            if (supported.topic.equals(envelope.getTopic()) && supported.eventType.equals(envelope.getEventType())) {
                return supported.payloadType;
            }
        }

        throw new IllegalStateException("Unsupported user outbox topic/event type. topic=%s eventType=%s outboxId=%s"
                .formatted(envelope.getTopic(), envelope.getEventType(), envelope.getId()));
    }

    private enum SupportedEvent {
        USER_CREATED(KafkaTopics.USER_EVENTS_V1, UserEvent.Type.USER_CREATED.name(), UserEvent.class),
        USER_UPDATED(KafkaTopics.USER_EVENTS_V1, UserEvent.Type.USER_UPDATED.name(), UserEvent.class),
        USER_SOFT_DELETE(KafkaTopics.USER_EVENTS_V1, UserEvent.Type.USER_SOFT_DELETE.name(), UserEvent.class),
        EMAIL_TO_SEND(KafkaTopics.EMAIL_EVENTS_V1, EmailToSendEvent.EVENT_TYPE, EmailToSendEvent.class),
        USER_AUTHENTICATED(KafkaTopics.USER_AUTHENTICATED_EVENTS_V1, UserAuthenticatedEvent.EVENT_TYPE,
                UserAuthenticatedEvent.class);

        private final String topic;
        private final String eventType;
        private final Class<? extends OutboxEvent> payloadType;

        SupportedEvent(String topic, String eventType, Class<? extends OutboxEvent> payloadType) {
            this.topic = topic;
            this.eventType = eventType;
            this.payloadType = payloadType;
        }
    }
}
