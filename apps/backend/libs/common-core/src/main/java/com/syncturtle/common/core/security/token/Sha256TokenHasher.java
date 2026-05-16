package com.syncturtle.common.core.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class Sha256TokenHasher implements TokenHasher {

    private static final String ALGORITHM = "SHA-256";

    @Override
    public String hash(String token) {
        Objects.requireNonNull(token, "token");

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalArgumentException(ALGORITHM + " is not available", exception);
        }
    }

}
