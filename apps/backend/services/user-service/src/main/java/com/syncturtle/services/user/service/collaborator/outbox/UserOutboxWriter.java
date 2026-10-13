package com.syncturtle.services.user.service.collaborator.outbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.user.event.UserOutboxEvent;
import com.syncturtle.services.user.model.OutboxMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserOutboxWriter {

    private static final String USER_AGGREGATE_TYPE = "User";

    private final OutboxMessageWriter writer;

    public OutboxMessage saveUserEvent(UserEvent event) {
        return save(event, USER_AGGREGATE_TYPE);
    }

    private OutboxMessage save(UserOutboxEvent event, String aggregateType) {
        Assert.notNull(event, "workspace outbox event is required");
        Assert.hasText(aggregateType, "aggregate type is required");

        return writer.save(event, event.messageKey(), aggregateType, event.aggregateId());
    }

}
