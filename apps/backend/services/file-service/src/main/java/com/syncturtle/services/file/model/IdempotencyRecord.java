package com.syncturtle.services.file.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "idempotency_record")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {

    public enum Status {
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "route_key", nullable = false, length = 128)
    private String routeKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status;

    @Column(name = "processing_token")
    private UUID processingToken;

    @Column(name = "processing_expires_at")
    private Instant processingExpiresAt;

    @Column(name = "http_status")
    private Integer httpStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static IdempotencyRecord processing(
            String idempotencyKey,
            UUID ownerUserId,
            String routeKey,
            String requestHash,
            Duration processingLease,
            Duration retention,
            Clock clock) {
        Assert.hasText(idempotencyKey, "idempotencyKey is required");
        Assert.notNull(ownerUserId, "ownerUserId is required");
        Assert.hasText(routeKey, "routeKey is required");
        Assert.hasText(requestHash, "requestHash is required");
        requirePositive(processingLease, "processingLease");
        requirePositive(retention, "retention");
        Assert.notNull(clock, "clock is required");

        IdempotencyRecord record = new IdempotencyRecord();
        record.id = UUID.randomUUID();
        record.idempotencyKey = idempotencyKey.trim();
        record.ownerUserId = ownerUserId;
        record.routeKey = routeKey.trim();
        record.requestHash = requestHash.trim();
        record.createdAt = Instant.now(clock);
        record.claim(processingLease, retention, clock);

        return record;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public boolean requestHashMatches(String candidateRequestHash) {
        return StringUtils.hasText(candidateRequestHash) && requestHash.equals(candidateRequestHash.trim());
    }

    public boolean isExpired(Clock clock) {
        Assert.notNull(clock, "clock is required");

        return !expiresAt.isAfter(Instant.now(clock));
    }

    public boolean hasActiveProcessingLease(Clock clock) {
        Assert.notNull(clock, "clock is required");

        return status == Status.PROCESSING && processingExpiresAt != null
                && processingExpiresAt.isAfter(Instant.now(clock));
    }

    public UUID reclaim(String newRequestHash, Duration processingLease, Duration retention, Clock clock) {
        Assert.hasText(newRequestHash, "requestHash is required");
        Assert.notNull(clock, "clock is required");

        if (!isExpired(clock)) {
            Assert.state(requestHashMatches(newRequestHash),
                    "an unexpired idempotency record cannot be reclaimed for a different request");
            Assert.state(isFailed() || !hasActiveProcessingLease(clock),
                    "an active idempotency request cannot be reclaimed");
        }

        requestHash = newRequestHash.trim();
        createdAt = Instant.now(clock);

        return claim(processingLease, retention, clock);
    }

    public boolean processingTokenMatches(UUID candidateToken) {
        return candidateToken != null && candidateToken.equals(processingToken);
    }

    public void complete(UUID expectedToken, int httpStatus, String responseBody, Duration retention, Clock clock) {
        Assert.isTrue(processingTokenMatches(expectedToken), "idempotency processing token does not match");
        Assert.state(status == Status.PROCESSING, "idempotency record is not processing");
        Assert.isTrue(httpStatus >= 200 && httpStatus < 300, "httpStatus must be successful");
        Assert.hasText(responseBody, "responseBody is required");
        requirePositive(retention, "retention");
        Assert.notNull(clock, "clock is required");

        Instant now = Instant.now(clock);
        this.status = Status.COMPLETED;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody.trim();
        this.completedAt = Instant.now(clock);
        this.expiresAt = now.plus(retention);
        this.processingToken = null;
        this.processingExpiresAt = null;
    }

    public void failIfClaimed(UUID expectedToken, Duration retention, Clock clock) {
        requirePositive(retention, "retention");
        Assert.notNull(clock, "clock is required");

        if (status != Status.PROCESSING || !processingTokenMatches(expectedToken)) {
            return;
        }

        status = Status.FAILED;
        httpStatus = null;
        responseBody = null;
        completedAt = null;
        expiresAt = Instant.now(clock).plus(retention);
        processingToken = null;
        processingExpiresAt = null;
    }

    private UUID claim(Duration processingLease, Duration retention, Clock clock) {
        requirePositive(processingLease, "processingLease");
        requirePositive(retention, "retention");

        Instant now = Instant.now(clock);
        this.status = Status.PROCESSING;
        this.processingToken = UUID.randomUUID();
        this.processingExpiresAt = now.plus(processingLease);
        this.expiresAt = now.plus(retention);
        this.httpStatus = null;
        this.responseBody = null;
        this.completedAt = null;

        return this.processingToken;
    }

    private static void requirePositive(Duration duration, String fieldName) {
        Assert.notNull(duration, fieldName + " is required");
        Assert.isTrue(!duration.isZero() && !duration.isNegative(), fieldName + " must be positive");
    }

}
