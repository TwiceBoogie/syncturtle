package com.syncturtle.services.workspace.messaging.outbox;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.services.workspace.model.OutboxMessage;
import com.syncturtle.services.workspace.model.param.OutboxMessageCreateParam;
import com.syncturtle.services.workspace.repository.OutboxMessageRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceOutboxWriter {

    private static final String WORKSPACE_AGGREGATE_TYPE = "Workspace";

    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxMessage saveWorkspaceEvent(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");

        String payload = serialize(event);

        OutboxMessageCreateParam param = OutboxMessageCreateParam.builder()
                .topic(KafkaTopics.WORKSPACE_EVENTS_V1)
                .messageKey(event.getId().toString())
                .eventType(event.getType().name())
                .aggregateType(WORKSPACE_AGGREGATE_TYPE)
                .aggregateId(event.getId())
                .payload(payload)
                .maxAttempts(20)
                .clock(clock)
                .build();

        return repository.save(OutboxMessage.create(param));
    }

    private String serialize(WorkspaceEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize workspace event for outbox.", exception);
        }
    }

}
