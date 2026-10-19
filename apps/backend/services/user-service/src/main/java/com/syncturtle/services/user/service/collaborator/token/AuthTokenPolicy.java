package com.syncturtle.services.user.service.collaborator.token;

import java.time.Duration;

public final class AuthTokenPolicy {

    public static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);
    public static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofMinutes(30);
    public static final int PASSWORD_RESET_TOKEN_BYTES = 48;

    private AuthTokenPolicy() {
    }
}
