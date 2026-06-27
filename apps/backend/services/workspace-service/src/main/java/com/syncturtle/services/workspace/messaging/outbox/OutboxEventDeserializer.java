package com.syncturtle.services.workspace.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberInviteEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceOutboxEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxEventDeserializer {

    private final ObjectMapper objectMapper;

    public WorkspaceOutboxEvent toWorkspaceEvent(OutboxEnvelope envelope) {
        Assert.notNull(envelope, "outbox envelope is required");

        try {
            return objectMapper.readValue(envelope.getPayload(), payloadType(envelope));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not deserialize workspace outbox event. outboxId=%s topic=%s eventType=%s"
                            .formatted(envelope.getId(), envelope.getTopic(), envelope.getEventType()),
                    exception);
        }
    }

    private static Class<? extends WorkspaceOutboxEvent> payloadType(OutboxEnvelope envelope) {
        return switch (envelope.getTopic()) {
            case KafkaTopics.WORKSPACE_EVENTS_V1 -> WorkspaceEvent.class;
            case KafkaTopics.WORKSPACE_MEMBER_EVENTS_V1 -> WorkspaceMemberEvent.class;
            case KafkaTopics.WORKSPACE_MEMBER_INVITE_EVENTS_V1 -> WorkspaceMemberInviteEvent.class;
            default ->
                throw new IllegalStateException("Unsupported workspace outbox topic. topic=%s eventType=%s outboxId=%s"
                        .formatted(envelope.getTopic(), envelope.getEventType(), envelope.getId()));
        };
    }

}
