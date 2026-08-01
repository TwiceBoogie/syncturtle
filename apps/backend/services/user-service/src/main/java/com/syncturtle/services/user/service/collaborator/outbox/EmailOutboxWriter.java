package com.syncturtle.services.user.service.collaborator.outbox;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.user.model.OutboxMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailOutboxWriter {

    private static final String USER_LOGIN_EMAIL_PASSWORD_AGGREGATE_TYPE = "User";

    private final OutboxMessageWriter writer;

    public OutboxMessage saveEmailToSendEvent(EmailToSendEvent event, UUID userId) {
        Assert.notNull(event, "email to send event is required");
        Assert.hasText(event.getEventId(), "email event id is required");
        Assert.notNull(event.getOccurredAt(), "email occurredAt is required");
        Assert.notNull(userId, "userId is required");

        return writer.save(event, KafkaTopics.EMAIL_EVENTS_V1, userId.toString(),
                USER_LOGIN_EMAIL_PASSWORD_AGGREGATE_TYPE, userId);
    }

}
