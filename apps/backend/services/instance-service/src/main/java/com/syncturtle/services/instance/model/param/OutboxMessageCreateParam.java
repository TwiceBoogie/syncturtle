package com.syncturtle.services.instance.model.param;

import java.time.Clock;
import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class OutboxMessageCreateParam {

    private static final int MAX_TOPIC_LENGTH = 255;
    private static final int MAX_MESSAGE_KEY_LENGTH = 255;
    private static final int MAX_EVENT_TYPE_LENGTH = 120;
    private static final int MAX_AGGREGATE_TYPE_LENGTH = 120;

    private final String topic;
    private final String messageKey;
    private final String eventType;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String payload;
    private final int maxAttempts;
    private final Clock clock;

    @Builder
    private OutboxMessageCreateParam(
            String topic,
            String messageKey,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            String payload,
            Integer maxAttempts,
            Clock clock) {
        this.topic = normalizeRequired(topic, "topic", MAX_TOPIC_LENGTH);
        this.messageKey = normalizeRequired(messageKey, "messageKey", MAX_MESSAGE_KEY_LENGTH);
        this.eventType = normalizeRequired(eventType, "eventType", MAX_EVENT_TYPE_LENGTH);
        this.aggregateType = normalizeRequired(aggregateType, "aggregateType", MAX_AGGREGATE_TYPE_LENGTH);
        this.aggregateId = requireId(aggregateId, "aggregateId is required");
        this.payload = normalizeRequired(payload, "payload", Integer.MAX_VALUE);
        this.maxAttempts = normalizeMaxAttempts(maxAttempts);
        this.clock = requireClock(clock);
    }

    private static UUID requireId(UUID value, String message) {
        Assert.notNull(value, message);
        return value;
    }

    private static Clock requireClock(Clock value) {
        Assert.notNull(value, "clock is required");
        return value;
    }

    private static int normalizeMaxAttempts(Integer value) {
        if (value == null) {
            return 20;
        }

        Assert.isTrue(value > 0, "maxAttempts must be greater than 0");
        Assert.isTrue(value <= 100, "maxAttempts must be less than or equal to 100");

        return value;
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

}
