package com.syncturtle.services.instance.service.collaborator.outbox;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceOutboxWriter")
class InstanceOutboxWriterTest {

    @Mock
    OutboxMessageWriter messageWriter;

    private InstanceOutboxWriter outboxWriter;

    @BeforeEach
    void setup() {
        outboxWriter = new InstanceOutboxWriter(messageWriter);
    }

    @Nested
    @DisplayName("saveInstanceEvent(InstanceEvent)")
    class SaveInstanceEventTests {

        @Test
        @DisplayName("keys configuration records by instance and scope")
        void keysByInstanceAndScope() {
            // arrange
            UUID instanceId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            InstanceConfigurationEvent event = InstanceConfigurationEvent.builder()
                    .eventId("event-1")
                    .occurredAt(Instant.parse("2026-07-29T12:00:00Z"))
                    .instanceId(instanceId)
                    .scope(InstanceConfigurationScope.EMAIL)
                    .configurationVersion(3L)
                    .changedKeys(Set.of("EMAIL_HOST"))
                    .build();
            // conditions
            // act
            outboxWriter.saveInstanceConfigurationEvent(event);
            // assert
            // verify
            verify(messageWriter).save(event, KafkaTopics.INSTANCE_CONFIG_EVENTS_V1, instanceId + ":EMAIL",
                    "InstanceConfiguration", instanceId);
        }

    }

}
