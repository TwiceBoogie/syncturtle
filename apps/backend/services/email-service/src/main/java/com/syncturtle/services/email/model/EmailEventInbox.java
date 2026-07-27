package com.syncturtle.services.email.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.model.param.EmailEventInboxCreateParam;
import com.syncturtle.services.email.model.param.EmailEventInboxRetryFailureParam;
import com.syncturtle.services.email.type.EmailEventInboxStatus;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "email_event_inbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailEventInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, updatable = false, length = 50)
    private EmailTemplateType templateType;

    @Column(name = "subject", nullable = false, updatable = false, length = 255)
    private String subject;

    @Column(name = "recipient_to_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String recipientToJson;

    @Column(name = "template_model_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String templateModelJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EmailEventInboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "lock_until")
    private Instant lockUntil;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static EmailEventInbox create(EmailEventInboxCreateParam param, Clock clock, Duration processingLease) {
        Assert.notNull(param, "email event inbox create param is required");
        Assert.notNull(clock, "clock is required");
        requirePositiveDuration(processingLease, "processingLease");

        Instant now = Instant.now(clock);
        EmailEventInbox inbox = new EmailEventInbox();
        inbox.eventId = param.getEventId();
        inbox.eventType = param.getEventType();
        inbox.templateType = param.getTemplateType();
        inbox.subject = param.getSubject();
        inbox.recipientToJson = param.getRecipientToJson();
        inbox.templateModelJson = param.getTemplateModelJson();
        inbox.status = EmailEventInboxStatus.PROCESSING;
        inbox.attemptCount = 1;
        inbox.lockUntil = now.plus(processingLease);
        inbox.nextAttemptAt = null;
        inbox.processedAt = null;
        inbox.lastError = null;
        inbox.createdAt = now;
        inbox.updatedAt = now;

        return inbox;
    }

    public boolean isSent() {
        return status == EmailEventInboxStatus.SENT;
    }

    public boolean isPermanentFailure() {
        return status == EmailEventInboxStatus.FAILED_PERMANENT;
    }

    public boolean hasActiveProcessingLease(Clock clock) {
        Assert.notNull(clock, "clock is required");

        return status == EmailEventInboxStatus.PROCESSING
                && lockUntil != null
                && lockUntil.isAfter(Instant.now(clock));
    }

    public boolean hasScheduledRetry(Clock clock) {
        Assert.notNull(clock, "clock is required");

        return status == EmailEventInboxStatus.FAILED_RETRYABLE
                && nextAttemptAt != null
                && nextAttemptAt.isAfter(Instant.now(clock));
    }

    public void claimForProcessing(Clock clock, Duration processingLease) {
        Assert.notNull(clock, "clock is required");
        requirePositiveDuration(processingLease, "processingLease");
        Assert.state(status == EmailEventInboxStatus.PROCESSING || status == EmailEventInboxStatus.FAILED_RETRYABLE,
                "Only PROCESSING or FAILED_RETRYABLE inbox rows may be claimed");

        Instant now = Instant.now(clock);

        if (status == EmailEventInboxStatus.PROCESSING) {
            Assert.state(lockUntil == null || !lockUntil.isAfter(now), "Email inbox processing lease is still active");
        }

        if (status == EmailEventInboxStatus.FAILED_RETRYABLE) {
            Assert.state(nextAttemptAt == null || !nextAttemptAt.isAfter(now), "Email inbox retry is not due");
        }

        status = EmailEventInboxStatus.PROCESSING;
        attemptCount = Math.max(1, attemptCount + 1);
        lockUntil = now.plus(processingLease);
        nextAttemptAt = null;
        processedAt = null;
        lastError = null;
        touch(now);
    }

    public void markSent(Clock clock) {
        Assert.notNull(clock, "clock is required");
        requireProcessing();

        Instant now = Instant.now(clock);
        status = EmailEventInboxStatus.SENT;
        processedAt = now;
        lockUntil = null;
        nextAttemptAt = null;
        lastError = null;
        touch(now);
    }

    public void markRetryableFailure(EmailEventInboxRetryFailureParam param, Clock clock) {
        Assert.notNull(param, "email event inbox retry failure param is required");
        Assert.notNull(clock, "clock is required");
        requireProcessing();

        Instant now = Instant.now(clock);

        if (attemptCount >= param.getMaxAttempts()) {
            status = EmailEventInboxStatus.FAILED_PERMANENT;
            processedAt = now;
            nextAttemptAt = null;
        } else {
            status = EmailEventInboxStatus.FAILED_RETRYABLE;
            processedAt = null;
            nextAttemptAt = now.plus(param.getRetryDelay());
        }

        lockUntil = null;
        lastError = param.getErrorMessage();
        touch(now);
    }

    public void markPermanentFailure(String errorMessage, Clock clock) {
        Assert.hasText(errorMessage, "errorMessage is required");
        Assert.isTrue(errorMessage.length() <= 4_000, "errorMessage must be 4000 characters or fewer");
        Assert.notNull(clock, "clock is required");
        requireProcessing();

        Instant now = Instant.now(clock);
        status = EmailEventInboxStatus.FAILED_PERMANENT;
        processedAt = now;
        lockUntil = null;
        nextAttemptAt = null;
        lastError = errorMessage;
        touch(now);
    }

    private void requireProcessing() {
        Assert.state(status == EmailEventInboxStatus.PROCESSING, "Email inbox row must be PROCESSING");
    }

    private void touch(Instant now) {
        Assert.notNull(now, "now is required");

        updatedAt = now;
    }

    private static void requirePositiveDuration(Duration duration, String fieldName) {
        Assert.notNull(duration, fieldName + " is required");
        Assert.isTrue(!duration.isNegative() && !duration.isZero(), fieldName + " must be positive");
    }

}
