package com.syncturtle.platform.gateway.support.fixture;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;

public final class RefreshSessionFamilyRecordFixtures {

    public static final String USER_ID = "c12c7988-b9fe-4299-a72f-e430d004d37b";
    public static final String INSTANCE_ID = "91ff9854-e8ac-4bf5-91d8-3117dcd8d05c";
    public static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");

    private RefreshSessionFamilyRecordFixtures() {
        throw new AssertionError("RefreshSessionFamilyRecordFixtures must not be instantiated");
    }

    public static RefreshSessionFamilyRecord validRecord() {
        Instant createdAt = NOW.minus(Duration.ofDays(1));
        Instant lastUsedAt = NOW.minus(Duration.ofHours(1));
        return family(createdAt, lastUsedAt, List.of("USER"), null);
    }

    public static RefreshSessionFamilyRecord validAdminRecord() {
        Instant createdAt = NOW.minus(Duration.ofDays(1));
        Instant lastUsedAt = NOW.minus(Duration.ofHours(1));
        return family(createdAt, lastUsedAt, List.of("INSTANCE_ADMIN", "USER"), 8L);
    }

    public static RefreshSessionFamilyRecord idleExpiredRecord() {
        Instant createdAt = NOW.minus(Duration.ofDays(10));
        Instant lastUsedAt = NOW.minus(Duration.ofDays(7));
        return family(createdAt, lastUsedAt, List.of("USER"), null);
    }

    public static RefreshSessionFamilyRecord absoluteExpiredRecord() {
        Instant createdAt = NOW.minus(Duration.ofDays(30));
        Instant lastUsedAt = NOW.minus(Duration.ofHours(1));
        return family(createdAt, lastUsedAt, List.of("USER"), null);
    }

    private static RefreshSessionFamilyRecord family(
            Instant createdAt,
            Instant lastUsedAt,
            List<String> roles,
            Long adminSessionVersion) {
        Instant absoluteExpiresAt = createdAt.plus(RefreshSessionFamilyRecord.ABSOLUTE_LIFETIME);
        Instant idleExpiresAt = lastUsedAt.plus(RefreshSessionFamilyRecord.IDLE_LIFETIME);
        if (idleExpiresAt.isAfter(absoluteExpiresAt)) {
            idleExpiresAt = absoluteExpiresAt;
        }

        return RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId(USER_ID)
                .instanceId(INSTANCE_ID)
                .roles(roles)
                .authVersion(4L)
                .adminSessionVersion(adminSessionVersion)
                .currentRefreshTokenHash("a".repeat(64))
                .rotationCounter(2L)
                .createdAt(createdAt)
                .lastUsedAt(lastUsedAt)
                .idleExpiresAt(idleExpiresAt)
                .absoluteExpiresAt(absoluteExpiresAt)
                .deviceLabel("Test browser")
                .clientBindingHash("b".repeat(64))
                .build();
    }
}
