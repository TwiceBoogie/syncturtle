package com.syncturtle.services.user.service.collaborator.outbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.user.event.UserAuthenticatedEvent;
import com.syncturtle.services.user.model.OutboxMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAuthenticationOutboxWriter {

    private static final String USER_AUTHENTICATION_AGGREGATE_TYPE = "UserAuthentication";

    private final OutboxMessageWriter writer;

    public OutboxMessage saveUserAuthenticatedEvent(UserAuthenticatedEvent event) {
        Assert.notNull(event, "user authenticated event is required");
        Assert.hasText(event.getEventId(), "user authenticated event id is required");
        Assert.notNull(event.getOccurredAt(), "user authenticated occurredAt is required");
        Assert.notNull(event.getUserId(), "userId is required");

        return writer.save(event, KafkaTopics.USER_AUTHENTICATED_EVENTS_V1, messageKey(event),
                USER_AUTHENTICATION_AGGREGATE_TYPE,
                event.getUserId());
    }

    private static String messageKey(UserAuthenticatedEvent event) {
        return event.getUserId().toString();
    }

}
