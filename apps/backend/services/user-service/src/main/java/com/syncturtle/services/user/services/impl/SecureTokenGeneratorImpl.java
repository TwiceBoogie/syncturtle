package com.syncturtle.services.user.services.impl;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.syncturtle.services.user.services.TokenGenerator;

@Component
public class SecureTokenGeneratorImpl implements TokenGenerator {

    private static final int TOKEN_BYTES = 64;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate(int size) {
        int tokenBytes = size < 16 ? TOKEN_BYTES : size;
        byte[] bytes = new byte[tokenBytes];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

}
