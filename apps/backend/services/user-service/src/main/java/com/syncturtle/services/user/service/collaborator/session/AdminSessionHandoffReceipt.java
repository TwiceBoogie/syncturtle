package com.syncturtle.services.user.service.collaborator.session;

import java.time.Duration;
import java.time.Instant;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class AdminSessionHandoffReceipt {

    private static final Duration MAXIMUM_LIFETIME = Duration.ofSeconds(30);

    private final String completionCode;
    private final Instant issuedAt;
    private final Instant expiresAt;

    public AdminSessionHandoffReceipt(String completionCode, Instant issuedAt, Instant expiresAt) {
        Assert.hasText(completionCode, "completionCode is required");
        Assert.notNull(issuedAt, "issuedAt is required");
        Assert.notNull(expiresAt, "expiresAt is required");
        Assert.isTrue(expiresAt.isAfter(issuedAt), "expiresAt must be after issuedAt");
        Assert.isTrue(Duration.between(issuedAt, expiresAt).compareTo(MAXIMUM_LIFETIME) <= 0,
                "handoff lifetime must not exceed 30 seconds");

        this.completionCode = completionCode.trim();
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

}
