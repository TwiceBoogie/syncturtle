package com.syncturtle.services.workspace.messaging.outbox;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.workspace.model.OutboxMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailOutboxWriter {

    private static final String WORKSPACE_MEMBER_INVITE_AGGREGATE_TYPE = "WorkspaceMemberInvite";

    private final OutboxMessageWriter writer;

    public OutboxMessage saveEmailToSendEvent(EmailToSendEvent event, UUID workspaceMemberInviteId) {
        Assert.notNull(event, "email to send event is required");
        Assert.hasText(event.getEventId(), "email event id is required");
        Assert.notNull(event.getOccurredAt(), "email occurredAt is required");
        Assert.notNull(workspaceMemberInviteId, "workspaceMemberInviteId is required");

        return writer.save(event, KafkaTopics.EMAIL_EVENTS_V1, workspaceMemberInviteId.toString(),
                WORKSPACE_MEMBER_INVITE_AGGREGATE_TYPE, workspaceMemberInviteId);
    }

}
