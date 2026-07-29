package com.syncturtle.services.instance.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.services.instance.messaging.outbox.OutboxEnvelope;
import com.syncturtle.services.instance.model.param.OutboxMessageCreateParam;
import com.syncturtle.services.instance.type.OutboxStatus;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "outbox_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage {

    private static final int MAX_TOPIC_LENGTH = 255;
    private static final int MAX_MESSAGE_KEY_LENGTH = 255;
    private static final int MAX_EVENT_TYPE_LENGTH = 120;
    private static final int MAX_AGGREGATE_TYPE_LENGTH = 120;
    private static final int MAX_TRACEPARENT_LENGTH = 55;
    private static final int MAX_TRACESTATE_LENGTH = 512;
    private static final int MAX_LOCKED_BY_LENGTH = 120;
    private static final int INITIAL_ATTEMPTS = 0;

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "topic", nullable = false, length = MAX_TOPIC_LENGTH)
    private String topic;

    @Column(name = "message_key", nullable = false, length = MAX_MESSAGE_KEY_LENGTH)
    private String messageKey;

    @Column(name = "event_type", nullable = false, length = MAX_EVENT_TYPE_LENGTH)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = MAX_AGGREGATE_TYPE_LENGTH)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "traceparent", length = MAX_TRACEPARENT_LENGTH)
    private String traceparent;

    @Column(name = "tracestate", length = MAX_TRACESTATE_LENGTH)
    private String tracestate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", length = MAX_LOCKED_BY_LENGTH)
    private String lockedBy;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public static OutboxMessage create(OutboxMessageCreateParam param) {
        Assert.notNull(param, "outbox create param is required");

        Instant now = Instant.now(param.getClock());

        OutboxMessage message = new OutboxMessage();
        message.topic = normalizeRequired(param.getTopic(), "topic", MAX_TOPIC_LENGTH);
        message.messageKey = normalizeRequired(param.getMessageKey(), "messageKey", MAX_MESSAGE_KEY_LENGTH);
        message.eventType = normalizeRequired(param.getEventType(), "eventType", MAX_EVENT_TYPE_LENGTH);
        message.aggregateType = normalizeRequired(param.getAggregateType(), "aggregateType", MAX_AGGREGATE_TYPE_LENGTH);
        message.aggregateId = requireId(param.getAggregateId(), "aggregateId is required");
        message.payload = normalizeRequired(param.getPayload(), "payload", Integer.MAX_VALUE);
        message.traceparent = normalizeNullable(param.getTraceparent());
        message.tracestate = normalizeNullable(param.getTracestate());
        message.status = OutboxStatus.NEW;
        message.attempts = INITIAL_ATTEMPTS;
        message.maxAttempts = param.getMaxAttempts();
        message.nextAttemptAt = now;
        message.createdAt = now;
        message.updatedAt = now;

        return message;
    }

    public boolean isDue(Instant now) {
        Assert.notNull(now, "now is required");

        return isPublishableStatus() && !nextAttemptAt.isAfter(now);
    }

    public boolean isPublishableStatus() {
        return status == OutboxStatus.NEW || status == OutboxStatus.FAILED;
    }

    public boolean isPublishing() {
        return status == OutboxStatus.PUBLISHING;
    }

    public boolean isTerminal() {
        return status == OutboxStatus.PUBLISHED || status == OutboxStatus.DEAD;
    }

    public void markPublishing(String workerId, Clock clock) {
        Assert.hasText(workerId, "workerId is required");
        Assert.notNull(clock, "clock is required");

        requirePublishable();

        Instant now = Instant.now(clock);

        this.status = OutboxStatus.PUBLISHING;
        this.lockedBy = normalizeRequired(workerId, "workerId", MAX_LOCKED_BY_LENGTH);
        this.lockedAt = now;
        this.updatedAt = now;
        this.lastError = null;
    }

    public void markPublished(Clock clock) {
        Assert.notNull(clock, "clock is required");

        requirePublishing();

        Instant now = Instant.now(clock);

        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
        this.lockedAt = null;
        this.lockedBy = null;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage, Duration retryDelay, Clock clock) {
        Assert.notNull(retryDelay, "retryDelay is required");
        Assert.notNull(clock, "clock is required");
        Assert.isTrue(!retryDelay.isNegative(), "retryDelay must not be negative");

        requirePublishing();

        Instant now = Instant.now(clock);

        this.attempts = attempts + 1;
        this.lastError = normalizeNullable(errorMessage);
        this.lockedAt = null;
        this.lockedBy = null;
        this.updatedAt = now;

        if (attempts >= maxAttempts) {
            this.status = OutboxStatus.DEAD;
            this.nextAttemptAt = now;
            return;
        }

        this.status = OutboxStatus.FAILED;
        this.nextAttemptAt = now.plus(retryDelay);
    }

    public void releaseExpiredLock(Duration lockTimeout, Clock clock) {
        Assert.notNull(lockTimeout, "lockTimeout is required");
        Assert.notNull(clock, "clock is required");
        Assert.isTrue(!lockTimeout.isNegative(), "lockTimeout must not be negative");

        if (!isPublishing()) {
            return;
        }

        if (lockedAt == null) {
            releaseToFailed(clock);
            return;
        }

        Instant now = Instant.now(clock);
        Instant expiresAt = lockedAt.plus(lockTimeout);

        if (expiresAt.isAfter(now)) {
            return;
        }

        releaseToFailed(clock);
    }

    public OutboxEnvelope toEnvelope() {
        requirePersisted();
        return new OutboxEnvelope(id, topic, messageKey, eventType, payload, traceparent, tracestate, attempts);
    }

    private void releaseToFailed(Clock clock) {
        Instant now = Instant.now(clock);
        this.status = OutboxStatus.FAILED;
        this.lockedAt = null;
        this.lockedBy = null;
        this.nextAttemptAt = now;
        this.updatedAt = now;
    }

    private void requirePublishable() {
        Assert.state(isPublishableStatus(), "outbox message is not publishable");
    }

    private void requirePublishing() {
        Assert.state(status == OutboxStatus.PUBLISHING, "outbox message is not publishing");
    }

    private void requirePersisted() {
        Assert.state(id != null, "outbox message must be persisted");
    }

    private static UUID requireId(UUID value, String message) {
        Assert.notNull(value, message);
        return value;
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

}
