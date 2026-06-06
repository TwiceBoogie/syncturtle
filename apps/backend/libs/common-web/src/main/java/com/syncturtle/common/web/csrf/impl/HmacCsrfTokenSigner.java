package com.syncturtle.common.web.csrf.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.syncturtle.common.web.csrf.CsrfTokenSigner;

public final class HmacCsrfTokenSigner implements CsrfTokenSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_SIGNING_KEY_BYTES = 32;
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DEC = Base64.getUrlDecoder();

    private final byte[] signingKey;

    public HmacCsrfTokenSigner(byte[] signingKey) {
        this.signingKey = requireSigningKey(signingKey);
    }

    @Override
    public String sign(String token) {
        String rawToken = requireText(token, "token");
        byte[] signature = hmac(rawToken.getBytes(StandardCharsets.UTF_8));
        return token + "." + B64URL.encodeToString(signature);
    }

    @Override
    public boolean verify(String tokenWithSignature) {
        String rawToken = extractToken(tokenWithSignature);
        String encodedSignature = extractSignature(tokenWithSignature);
        if (rawToken == null || encodedSignature == null) {
            return false;
        }

        byte[] providedSignature;
        try {
            providedSignature = B64URL_DEC.decode(encodedSignature);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        byte[] expectedSignature = hmac(rawToken.getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(expectedSignature, providedSignature);
    }

    @Override
    public String extractToken(String tokenWithSignature) {
        if (tokenWithSignature == null) {
            return null;
        }

        int separatorIndex = tokenWithSignature.lastIndexOf('.');
        if (separatorIndex <= 0 || separatorIndex == tokenWithSignature.length() - 1) {
            return null;
        }

        return tokenWithSignature.substring(0, separatorIndex);
    }

    public static byte[] decodeSigningKeyBase64Url(String value) {
        String encodedKey = requireText(value, "b64url");

        try {
            return B64URL_DEC.decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "CSRF signing key must be valid Base64 URL without padding", exception);
        }
    }

    private String extractSignature(String tokenWithSignature) {
        if (tokenWithSignature == null) {
            return null;
        }

        int separatorIndex = tokenWithSignature.lastIndexOf('.');
        if (separatorIndex <= 0 || separatorIndex == tokenWithSignature.length() - 1) {
            return null;
        }

        return tokenWithSignature.substring(separatorIndex + 1);
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to compute HMAC using " + HMAC_ALGORITHM, exception);
        }
    }

    private static byte[] requireSigningKey(byte[] signingKey) {
        byte[] copy = Objects.requireNonNull(signingKey, "signingKey").clone();

        if (copy.length < MIN_SIGNING_KEY_BYTES) {
            throw new IllegalStateException(
                    "CSRF signing key too short. Use at least " + MIN_SIGNING_KEY_BYTES + " bytes.");
        }

        return copy;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value.trim();
    }

}
