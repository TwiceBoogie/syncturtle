package com.syncturtle.common.contracts.auth.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Pattern;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class PreAuthTransactionBinding {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private final String value;

    @Builder
    @Jacksonized
    private PreAuthTransactionBinding(String value) {
        String normalized = requireText(value, "pre-auth transaction binding");
        if (!SHA_256.matcher(normalized).matches()) {
            throw new IllegalArgumentException("pre-auth transaction binding must be a lowercase SHA-256 value");
        }

        this.value = normalized;
    }

    public static PreAuthTransactionBinding fromValidatedSignedCsrfToken(String signedToken) {
        String normalizedToken = requireText(signedToken, "validated signed CSRF token");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedToken.getBytes(StandardCharsets.UTF_8));
            return new PreAuthTransactionBinding(HexFormat.of().formatHex(hash));
        } catch (Exception exception) {
            throw new IllegalStateException("pre-auth transaction binding could not be computed", exception);
        }
    }

    public static PreAuthTransactionBinding fromHash(String value) {
        return new PreAuthTransactionBinding(value);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

}
