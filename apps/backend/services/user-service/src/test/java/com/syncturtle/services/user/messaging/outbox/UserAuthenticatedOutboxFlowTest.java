package com.syncturtle.services.user.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.messaging.OutboxEvent;
import com.syncturtle.common.contracts.user.event.UserAuthenticatedEvent;
import com.syncturtle.common.observability.tracing.PersistedTraceContext;
import com.syncturtle.common.observability.tracing.TraceContextPropagator;
import com.syncturtle.services.user.messaging.kafka.publisher.OutboxKafkaPublisher;
import com.syncturtle.services.user.model.OutboxMessage;
import com.syncturtle.services.user.repository.OutboxMessageRepository;
import com.syncturtle.services.user.service.collaborator.outbox.OutboxMessageClaimer;
import com.syncturtle.services.user.service.collaborator.outbox.OutboxMessageRecorder;
import com.syncturtle.services.user.service.collaborator.outbox.OutboxMessageWriter;
import com.syncturtle.services.user.service.collaborator.outbox.OutboxRetryDelayPolicy;
import com.syncturtle.services.user.service.collaborator.outbox.UserAuthenticationOutboxWriter;
import com.syncturtle.services.user.service.impl.OutboxRelayServiceImpl;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("USER_AUTHENTICATED outbox flow")
class UserAuthenticatedOutboxFlowTest {

    @Test
    void persistsCanonicalRowAndRelaysExistingRetryWithoutResettingIt() {
        // arrange: write a real payload using the application writer
        UserAuthenticatedEvent event = UserOutboxEventMappingTest.authenticatedEvent();
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        OutboxMessageRepository repository = Mockito.mock(OutboxMessageRepository.class);
        TraceContextPropagator traces = Mockito.mock(TraceContextPropagator.class);
        when(traces.capture()).thenReturn(new PersistedTraceContext(null, null));
        when(repository.save(any(OutboxMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
        OutboxMessageWriter messageWriter = new OutboxMessageWriter(repository, jsonMapper, traces, clock);
        UserAuthenticationOutboxWriter authenticationWriter = new UserAuthenticationOutboxWriter(messageWriter);

        // act: persist, then replay the stored payload as an already retrying row
        OutboxMessage stored = authenticationWriter.saveUserAuthenticatedEvent(event);
        UUID rowId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        OutboxEnvelope retrying = new OutboxEnvelope(rowId, stored.getTopic(), stored.getMessageKey(),
                stored.getEventType(), stored.getPayload(), null, null, 9);
        OutboxMessageClaimer claimer = Mockito.mock(OutboxMessageClaimer.class);
        OutboxMessageRecorder recorder = Mockito.mock(OutboxMessageRecorder.class);
        OutboxRetryDelayPolicy retryDelayPolicy = Mockito.mock(OutboxRetryDelayPolicy.class);
        OutboxKafkaPublisher publisher = Mockito.mock(OutboxKafkaPublisher.class);
        when(claimer.claimDueMessages("worker", 100)).thenReturn(List.of(retrying));
        OutboxRelayServiceImpl relay = new OutboxRelayServiceImpl(claimer, recorder,
                new OutboxEventDeserializer(jsonMapper), retryDelayPolicy, publisher);
        relay.relayDueMessages("worker");

        // assert: persisted contract and Kafka publication agree
        assertThat(stored.getTopic()).isEqualTo(KafkaTopics.USER_AUTHENTICATED_EVENTS_V1);
        assertThat(stored.getEventType()).isEqualTo(UserAuthenticatedEvent.EVENT_TYPE);
        assertThat(stored.getMessageKey()).isEqualTo(event.getUserId().toString());
        assertThat(stored.getAttempts()).isZero();
        ArgumentCaptor<OutboxEvent> published = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(publisher).publishOutboxEvent(eq(KafkaTopics.USER_AUTHENTICATED_EVENTS_V1),
                eq(event.getUserId().toString()), published.capture(), eq(new PersistedTraceContext(null, null)));
        assertThat(published.getValue()).isInstanceOf(UserAuthenticatedEvent.class);
        assertThat(published.getValue().getEventId()).isEqualTo(event.getEventId());
        verify(recorder).markPublished(rowId);
        verify(recorder, never()).markFailed(eq(rowId), any(), any());
    }
}
