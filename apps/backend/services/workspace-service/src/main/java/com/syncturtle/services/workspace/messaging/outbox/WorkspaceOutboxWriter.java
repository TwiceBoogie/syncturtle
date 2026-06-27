package com.syncturtle.services.workspace.messaging.outbox;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberInviteEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceOutboxEvent;
import com.syncturtle.services.workspace.model.OutboxMessage;
import com.syncturtle.services.workspace.model.param.OutboxMessageCreateParam;
import com.syncturtle.services.workspace.repository.OutboxMessageRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceOutboxWriter {

    private static final String WORKSPACE_AGGREGATE_TYPE = "Workspace";
    private static final String WORKSPACE_MEMBER_AGGREGATE_TYPE = "WorkspaceMember";
    private static final String WORKSPACE_MEMBER_INVITE_AGGREGATE_TYPE = "WorkspaceMemberInvite";

    private static final int DEFAULT_MAX_ATTEMPTS = 20;

    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxMessage saveWorkspaceEvent(WorkspaceEvent event) {
        return save(event, WORKSPACE_AGGREGATE_TYPE, KafkaTopics.WORKSPACE_EVENTS_V1);
    }

    public OutboxMessage saveWorkspaceMemberEvent(WorkspaceMemberEvent event) {
        return save(event, WORKSPACE_MEMBER_AGGREGATE_TYPE, KafkaTopics.WORKSPACE_MEMBER_EVENTS_V1);
    }

    public OutboxMessage saveWorkspaceMemberInviteEvent(WorkspaceMemberInviteEvent event) {
        return save(event, WORKSPACE_MEMBER_INVITE_AGGREGATE_TYPE, KafkaTopics.WORKSPACE_MEMBER_INVITE_EVENTS_V1);
    }

    private OutboxMessage save(WorkspaceOutboxEvent event, String aggregateType, String kafkaTopic) {
        Assert.notNull(event, "workspace outbox event is required");
        Assert.hasText(aggregateType, "aggregate type is required");
        Assert.hasText(kafkaTopic, "kafkaTopic is required");

        OutboxMessageCreateParam param = OutboxMessageCreateParam.builder()
                .topic(kafkaTopic)
                .messageKey(event.messageKey())
                .eventType(event.eventTypeName())
                .aggregateType(aggregateType)
                .aggregateId(event.aggregateId())
                .payload(serialize(event))
                .maxAttempts(DEFAULT_MAX_ATTEMPTS)
                .clock(clock)
                .build();

        return repository.save(OutboxMessage.create(param));
    }

    private String serialize(WorkspaceOutboxEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize workspace outbox event. eventType=%s, aggregateId=%s"
                    .formatted(event.eventTypeName(), event.aggregateId()), exception);
        }
    }

}
