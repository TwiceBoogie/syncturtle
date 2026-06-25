package com.syncturtle.services.file.model;

import java.time.Clock;
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
            Instant expiresAt,
            Clock clock) {
        Assert.hasText(idempotencyKey, "idempotencyKey is required");
        Assert.notNull(ownerUserId, "ownerUserId is required");
        Assert.hasText(routeKey, "routeKey is required");
        Assert.hasText(requestHash, "requestHash is required");
        Assert.notNull(expiresAt, "expiresAt is required");
        Assert.notNull(clock, "clock is required");

        IdempotencyRecord record = new IdempotencyRecord();
        record.id = UUID.randomUUID();
        record.idempotencyKey = idempotencyKey.trim();
        record.ownerUserId = ownerUserId;
        record.routeKey = routeKey.trim();
        record.requestHash = requestHash.trim();
        record.status = Status.PROCESSING;
        record.createdAt = Instant.now(clock);
        record.expiresAt = expiresAt;
        return record;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isProcessing() {
        return status == Status.PROCESSING;
    }

    public boolean requestHashMatches(String requestHash) {
        return StringUtils.hasText(requestHash) && this.requestHash.equals(requestHash.trim());
    }

    public void complete(int httpStatus, String responseBody, Clock clock) {
        Assert.isTrue(httpStatus >= 200 && httpStatus < 300, "httpStatus must be successful");
        Assert.hasText(responseBody, "responseBody is required");
        Assert.notNull(clock, "clock is required");

        this.status = Status.COMPLETED;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody.trim();
        this.completedAt = Instant.now(clock);
    }

}
