package com.syncturtle.services.user.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.messaging.OutboxEvent;
import com.syncturtle.common.contracts.user.event.UserAuthenticatedEvent;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.core.actor.PrincipalType;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("UserOutboxEventMapping and stored payloads")
class UserOutboxEventMappingTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T12:00:00Z");
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();
    private final OutboxEventDeserializer deserializer = new OutboxEventDeserializer(JSON_MAPPER);

    @ParameterizedTest(name = "{0} on {1}")
    @MethodSource("supportedEvents")
    void roundTripsEverySupportedEvent(OutboxEvent event, String topic, Class<? extends OutboxEvent> payloadType)
            throws Exception {
        // arrange: use the same JSON serializer as the outbox writer
        String storedPayload = JSON_MAPPER.writeValueAsString(event);
        OutboxEnvelope envelope = envelope(topic, event.eventTypeName(), storedPayload);

        // act
        String selectedTopic = UserOutboxEventMapping.topicFor(event);
        Class<? extends OutboxEvent> selectedType = UserOutboxEventMapping.payloadType(envelope);
        OutboxEvent restored = deserializer.toEvent(envelope);

        // assert
        assertThat(selectedTopic).isEqualTo(topic);
        assertThat(selectedType).isEqualTo(payloadType);
        assertThat(restored).isInstanceOf(payloadType);
        assertThat(restored.getEventId()).isEqualTo(event.getEventId());
        assertThat(restored.eventTypeName()).isEqualTo(event.eventTypeName());
        assertThat(JSON_MAPPER.writeValueAsString(restored)).isEqualTo(storedPayload);
    }

    @Test
    void rejectsTopicEventMismatchBeforeDeserializing() throws Exception {
        // arrange
        UserAuthenticatedEvent event = authenticatedEvent();
        String payload = JSON_MAPPER.writeValueAsString(event);
        OutboxEnvelope envelope = envelope(KafkaTopics.USER_EVENTS_V1, event.eventTypeName(), payload);

        // act/assert
        assertThatThrownBy(() -> deserializer.toEvent(envelope))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported user outbox topic/event type");
    }

    @Test
    void rejectsUnknownTopicAndEventType() throws Exception {
        // arrange
        String payload = JSON_MAPPER.writeValueAsString(authenticatedEvent());
        OutboxEnvelope unknownTopic = envelope("unknown.events.v1", UserAuthenticatedEvent.EVENT_TYPE, payload);
        OutboxEnvelope unknownType = envelope(KafkaTopics.USER_AUTHENTICATED_EVENTS_V1, "UNKNOWN", payload);

        // act/assert
        assertThatThrownBy(() -> deserializer.toEvent(unknownTopic)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> deserializer.toEvent(unknownType)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsPayloadWhoseEventTypeDisagreesWithRow() throws Exception {
        // arrange
        String payload = JSON_MAPPER.writeValueAsString(userEvent(UserEvent.Type.USER_UPDATED));
        OutboxEnvelope envelope = envelope(KafkaTopics.USER_EVENTS_V1, UserEvent.Type.USER_CREATED.name(), payload);

        // act/assert
        assertThatThrownBy(() -> deserializer.toEvent(envelope))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("payload event type does not match");
    }

    @Test
    void rejectsUnsupportedEventBeforeWriting() {
        // arrange
        OutboxEvent unsupported = new OutboxEvent() {
            @Override
            public String getEventId() {
                return "unsupported-1";
            }

            @Override
            public Instant getOccurredAt() {
                return OCCURRED_AT;
            }

            @Override
            public String eventTypeName() {
                return "UNSUPPORTED";
            }
        };

        // act/assert
        assertThatThrownBy(() -> UserOutboxEventMapping.topicFor(unsupported))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported user outbox event");
    }

    private static Stream<Arguments> supportedEvents() {
        return Stream.of(
                Arguments.of(userEvent(UserEvent.Type.USER_CREATED), KafkaTopics.USER_EVENTS_V1, UserEvent.class),
                Arguments.of(userEvent(UserEvent.Type.USER_UPDATED), KafkaTopics.USER_EVENTS_V1, UserEvent.class),
                Arguments.of(userEvent(UserEvent.Type.USER_SOFT_DELETE), KafkaTopics.USER_EVENTS_V1, UserEvent.class),
                Arguments.of(emailEvent(), KafkaTopics.EMAIL_EVENTS_V1, EmailToSendEvent.class),
                Arguments.of(authenticatedEvent(), KafkaTopics.USER_AUTHENTICATED_EVENTS_V1,
                        UserAuthenticatedEvent.class));
    }

    static UserAuthenticatedEvent authenticatedEvent() {
        return UserAuthenticatedEvent.builder()
                .eventId("authenticated-1")
                .occurredAt(OCCURRED_AT)
                .userId(USER_ID)
                .email("user@example.test")
                .createdUser(false)
                .provider("EMAIL_PASSWORD")
                .build();
    }

    private static EmailToSendEvent emailEvent() {
        return EmailToSendEvent.builder()
                .eventId("email-1")
                .occurredAt(OCCURRED_AT)
                .templateType(EmailTemplateType.MAGIC_CODE)
                .subject("Magic code")
                .to(List.of("user@example.test"))
                .model(Map.of("code", "123456"))
                .build();
    }

    private static UserEvent userEvent(UserEvent.Type type) {
        return UserEvent.builder()
                .eventId("user-" + type.name())
                .occurredAt(OCCURRED_AT)
                .type(type)
                .id(USER_ID)
                .username("user")
                .email("user@example.test")
                .displayName("User")
                .active(type != UserEvent.Type.USER_SOFT_DELETE)
                .emailVerified(true)
                .passwordAutoset(false)
                .userTimezone("America/Chicago")
                .principalType(PrincipalType.HUMAN)
                .authVersion(0L)
                .createdAt(OCCURRED_AT)
                .updatedAt(OCCURRED_AT)
                .version(0L)
                .build();
    }

    static OutboxEnvelope envelope(String topic, String eventType, String payload) {
        return new OutboxEnvelope(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), topic,
                USER_ID.toString(), eventType, payload, null, null, 9);
    }
}
