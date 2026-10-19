package com.syncturtle.services.user.service.collaborator.session;

import java.time.Instant;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;

import lombok.Getter;

@Getter
public final class RefreshSessionReceipt {

    private final String sessionId;
    private final String refreshToken;
    private final RefreshSessionRecord record;
    private final Instant issuedAt;

    public RefreshSessionReceipt(String sessionId, String refreshToken, RefreshSessionRecord record, Instant issuedAt) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.hasText(refreshToken, "refreshToken is required");
        Assert.notNull(record, "record is required");
        Assert.notNull(issuedAt, "issuedAt is required");

        this.sessionId = sessionId;
        this.refreshToken = refreshToken;
        this.record = record;
        this.issuedAt = issuedAt;
    }

}
