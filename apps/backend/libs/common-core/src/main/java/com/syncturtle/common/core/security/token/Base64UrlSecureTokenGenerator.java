package com.syncturtle.common.core.security.token;

import java.security.SecureRandom;
import java.util.Base64;

public final class Base64UrlSecureTokenGenerator implements SecureTokenGenerator {

    private static final int MIN_TOKEN_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateBase64Url(int bytes) {
        if (bytes < MIN_TOKEN_BYTES) {
            throw new IllegalArgumentException(
                    "Token size must be at least " + MIN_TOKEN_BYTES + " bytes");
        }

        byte[] buffer = new byte[bytes];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(buffer);
    }

}
