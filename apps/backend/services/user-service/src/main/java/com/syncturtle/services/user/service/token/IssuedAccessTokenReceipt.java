package com.syncturtle.services.user.service.token;

import java.time.Instant;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IssuedAccessTokenReceipt {

    String token;
    Instant issuedAt;
    Instant expiresAt;

    private IssuedAccessTokenReceipt(String token, Instant issuedAt, Instant expiresAt) {
        Assert.hasText(token, "token is required");
        Assert.notNull(issuedAt, "issuedAt is required");
        Assert.notNull(expiresAt, "expiresAt is required");

        this.token = token;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

}
