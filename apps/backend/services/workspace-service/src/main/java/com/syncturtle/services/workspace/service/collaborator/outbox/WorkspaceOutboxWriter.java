package com.syncturtle.services.workspace.service.collaborator.outbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberInviteEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceOutboxEvent;
import com.syncturtle.services.workspace.model.OutboxMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceOutboxWriter {

    private static final String WORKSPACE_AGGREGATE_TYPE = "Workspace";
    private static final String WORKSPACE_MEMBER_AGGREGATE_TYPE = "WorkspaceMember";
    private static final String WORKSPACE_MEMBER_INVITE_AGGREGATE_TYPE = "WorkspaceMemberInvite";

    private final OutboxMessageWriter writer;

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

        return writer.save(event, kafkaTopic, event.messageKey(), aggregateType, event.aggregateId());
    }

}
