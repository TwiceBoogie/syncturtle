package com.syncturtle.services.user.service.collaborator.session;

import java.time.Clock;
import java.time.Instant;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.services.user.configuration.property.RefreshSessionLifecycleProperties;
import com.syncturtle.services.user.type.RefreshSessionLifetimeStatus;

public final class RefreshSessionLifetimeDecider {

    private final RefreshSessionLifecycleProperties properties;
    private final Clock clock;

    public RefreshSessionLifetimeDecider(RefreshSessionLifecycleProperties properties, Clock clock) {
        Assert.notNull(properties, "refresh session lifecycle propertis is required");
        Assert.notNull(clock, "clock is required");

        this.properties = properties;
        this.clock = clock;
    }

    public RefreshSessionLifetimeDecision decideForCreation() {
        Instant now = Instant.now(clock);
        Instant absoluteExpiresAt = now.plus(properties.getAbsoluteLifetime());
        Instant idleExpiresAt = minimum(now.plus(properties.getIdleLifetime()), absoluteExpiresAt);

        return RefreshSessionLifetimeDecision.builder()
                .status(RefreshSessionLifetimeStatus.CREATED)
                .effectiveNow(now)
                .createdAt(now)
                .lastUsedAt(now)
                .idleExpiresAt(idleExpiresAt)
                .absoluteExpiresAt(absoluteExpiresAt)
                .build();
    }

    public RefreshSessionLifetimeDecision decideForRotation(RefreshSessionFamilyRecord record) {
        Assert.notNull(record, "refresh session family record is required");

        Instant now = Instant.now(clock);
        // session lifecycle time remains monotonic from the apps perspective
        Instant effectiveNow = maximum(now, record.getLastUsedAt());

        if (!record.getAbsoluteExpiresAt().isAfter(effectiveNow)) {
            return existingDecision(RefreshSessionLifetimeStatus.ABSOLUTE_EXPIRED, effectiveNow, record);
        }

        if (!record.getIdleExpiresAt().isAfter(effectiveNow)) {
            return existingDecision(RefreshSessionLifetimeStatus.IDLE_EXPIRED, effectiveNow, record);
        }

        Instant idleExpiresAt = minimum(effectiveNow.plus(properties.getIdleLifetime()), record.getAbsoluteExpiresAt());

        return RefreshSessionLifetimeDecision.builder()
                .status(RefreshSessionLifetimeStatus.ROTATABLE)
                .effectiveNow(effectiveNow)
                .createdAt(record.getCreatedAt())
                .lastUsedAt(effectiveNow)
                .idleExpiresAt(idleExpiresAt)
                .absoluteExpiresAt(record.getAbsoluteExpiresAt())
                .build();
    }

    private static RefreshSessionLifetimeDecision existingDecision(
            RefreshSessionLifetimeStatus status,
            Instant effectiveNow,
            RefreshSessionFamilyRecord record) {
        return RefreshSessionLifetimeDecision.builder()
                .status(status)
                .effectiveNow(effectiveNow)
                .createdAt(record.getCreatedAt())
                .lastUsedAt(record.getLastUsedAt())
                .idleExpiresAt(record.getIdleExpiresAt())
                .absoluteExpiresAt(record.getAbsoluteExpiresAt())
                .build();
    }

    private static Instant minimum(Instant first, Instant second) {
        if (first.isBefore(second)) {
            return first;
        }
        return second;
    }

    private static Instant maximum(Instant first, Instant second) {
        if (first.isAfter(second)) {
            return first;
        }
        return second;
    }

}
