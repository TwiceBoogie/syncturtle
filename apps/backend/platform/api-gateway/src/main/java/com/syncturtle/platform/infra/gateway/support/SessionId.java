package com.syncturtle.platform.infra.gateway.support;

import java.security.SecureRandom;
import java.util.Base64;

public final class SessionId {

    private static final SecureRandom RNG = new SecureRandom();

    private SessionId() {
    }

    public static String newId() {
        byte[] bytes = new byte[32]; // 256 bit
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
