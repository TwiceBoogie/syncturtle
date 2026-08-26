package com.syncturtle.services.user.service.collaborator.session;

import java.time.Instant;

import org.springframework.util.Assert;

import com.syncturtle.services.user.type.RefreshSessionLifetimeStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class RefreshSessionLifetimeDecision {

    private final RefreshSessionLifetimeStatus status;
    private final Instant effectiveNow;
    private final Instant createdAt;
    private final Instant lastUsedAt;
    private final Instant idleExpiresAt;
    private final Instant absoluteExpiresAt;

    @Builder
    private RefreshSessionLifetimeDecision(
            RefreshSessionLifetimeStatus status,
            Instant effectiveNow,
            Instant createdAt,
            Instant lastUsedAt,
            Instant idleExpiresAt,
            Instant absoluteExpiresAt) {
        Assert.notNull(status, "status is required");
        Assert.notNull(effectiveNow, "effectiveNow is required");
        Assert.notNull(createdAt, "createdAt is required");
        Assert.notNull(lastUsedAt, "lastUsedAt is required");
        Assert.notNull(idleExpiresAt, "idleExpiresAt is required");
        Assert.notNull(absoluteExpiresAt, "absoluteExpiresAt is required");

        Assert.isTrue(!lastUsedAt.isBefore(createdAt), "lastUsedAt must not be before createdAt");
        Assert.isTrue(!effectiveNow.isBefore(lastUsedAt), "effectiveNow must not be before lastUsedAt");
        Assert.isTrue(absoluteExpiresAt.isAfter(createdAt), "absoluteExpiresAt must be after createdAt");
        Assert.isTrue(idleExpiresAt.isAfter(lastUsedAt), "idleExpiresAt must be after lastUsedAt");
        Assert.isTrue(!idleExpiresAt.isAfter(absoluteExpiresAt), "idleExpiresAt must not be after absoluteExpiresAt");

        this.status = status;
        this.effectiveNow = effectiveNow;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.idleExpiresAt = idleExpiresAt;
        this.absoluteExpiresAt = absoluteExpiresAt;
    }

}
