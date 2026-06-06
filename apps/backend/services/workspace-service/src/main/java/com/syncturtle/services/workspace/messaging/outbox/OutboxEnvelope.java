package com.syncturtle.services.workspace.messaging.outbox;

import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Getter;

/**
 * The detached object the scheduler publishes. Better than passing Jpa entities
 * around after claim
 */
@Getter
public final class OutboxEnvelope {

    private final UUID id;
    private final String topic;
    private final String messageKey;
    private final String eventType;
    private final String payload;
    private final int attempts;

    public OutboxEnvelope(
            UUID id,
            String topic,
            String messageKey,
            String eventType,
            String payload,
            int attempts) {
        Assert.notNull(id, "outbox id is required");
        Assert.hasText(topic, "topic is required");
        Assert.hasText(messageKey, "messageKey is required");
        Assert.hasText(eventType, "eventType is required");
        Assert.hasText(payload, "payload is required");
        Assert.isTrue(attempts >= 0, "attempts must be greater than or equal to 0");

        this.id = id;
        this.topic = topic;
        this.messageKey = messageKey;
        this.eventType = eventType;
        this.payload = payload;
        this.attempts = attempts;
    }

}
