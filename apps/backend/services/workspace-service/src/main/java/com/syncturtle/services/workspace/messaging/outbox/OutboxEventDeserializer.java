package com.syncturtle.services.workspace.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxEventDeserializer {

    private final ObjectMapper objectMapper;

    public WorkspaceEvent toWorkspaceEvent(OutboxEnvelope envelope) {
        Assert.notNull(envelope, "outbox envelope is required");

        try {
            return objectMapper.readValue(envelope.getPayload(), WorkspaceEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not deserialize workspace outbox event. outboxId=" + envelope.getId(), exception);
        }
    }

}
