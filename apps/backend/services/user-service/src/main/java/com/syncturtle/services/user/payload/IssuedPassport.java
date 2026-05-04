package com.syncturtle.services.user.payload;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class IssuedPassport {
    private final String token;
    private final Instant issuedAt;
    private final Instant expiresAt;
}
