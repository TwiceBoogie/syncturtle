package com.syncturtle.services.user.service.collaborator.session;

import java.time.Instant;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Value;

@Value
public class IssuedRefreshTokenReceipt {

    String sessionId;
    String token;
    Instant issuedAt;
    Instant expiresAt;

    @Builder
    private IssuedRefreshTokenReceipt(
            String sessionId,
            String token,
            Instant issuedAt,
            Instant expiresAt) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.hasText(token, "token is required");
        Assert.notNull(issuedAt, "issuedAt is required");
        Assert.notNull(expiresAt, "expiresAt is required");

        this.sessionId = sessionId;
        this.token = token;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

}
