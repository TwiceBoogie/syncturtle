package com.syncturtle.common.web.security.csrf;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacCsrfTokenSigner implements CsrfTokenSigner {

    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DEC = Base64.getUrlDecoder();

    private final String hmacAlgorithm;
    private final byte[] signingKey;

    public HmacCsrfTokenSigner(String hmacAlgorithm, byte[] signingKey) {
        this.hmacAlgorithm = Objects.requireNonNull(hmacAlgorithm, "hmacAlgorithm");
        this.signingKey = Objects.requireNonNull(signingKey, "signingKey").clone();
        if (this.signingKey.length < 32) {
            throw new IllegalStateException("CSRF signing key too short. Use at least 32 bytes (256-bit).");
        }
    }

    @Override
    public String sign(String token) {
        Objects.requireNonNull(token, "token");
        byte[] sig = hmac(token.getBytes(StandardCharsets.UTF_8));
        return token + "." + B64URL.encodeToString(sig);
    }

    @Override
    public boolean verify(String tokenWithSignature) {
        String token = extractToken(tokenWithSignature);
        String sigB64 = extractSignature(tokenWithSignature);
        if (token == null || sigB64 == null) {
            return false;
        }
        byte[] providedSig;
        try {
            providedSig = B64URL_DEC.decode(sigB64);
        } catch (Exception e) {
            return false;
        }
        byte[] expectedSig = hmac(token.getBytes(StandardCharsets.UTF_8));
        return constantTimeEquals(expectedSig, providedSig);
    }

    @Override
    public String extractToken(String tokenWithSignature) {
        if (tokenWithSignature == null) {
            return null;
        }
        int dot = tokenWithSignature.lastIndexOf('.');
        if (dot <= 0 || dot == tokenWithSignature.length() - 1) {
            return null;
        }
        return tokenWithSignature.substring(0, dot);
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(new SecretKeySpec(signingKey, hmacAlgorithm));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC using " + hmacAlgorithm, e);
        }
    }

    private String extractSignature(String tokenWithSignature) {
        if (tokenWithSignature == null) {
            return null;
        }
        int dot = tokenWithSignature.lastIndexOf('.');
        if (dot <= 0 || dot == tokenWithSignature.length() - 1) {
            return null;
        }
        return tokenWithSignature.substring(dot + 1);
    }

    private boolean constantTimeEquals(byte[] expectedSig, byte[] providedSig) {
        if (expectedSig == null || providedSig == null) {
            return false;
        }
        int diff = expectedSig.length ^ providedSig.length;
        for (int i = 0; i < Math.min(expectedSig.length, providedSig.length); i++) {
            diff |= expectedSig[i] ^ providedSig[i];
        }
        return diff == 0;
    }

    public static String generateRawToken(int bytes) {
        if (bytes < 16) {
            throw new IllegalArgumentException("Use at least 16 bytes");
        }
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return B64URL.encodeToString(buf);
    }

    public static byte[] decodeSigningKeyBase64Url(String b64url) {
        Objects.requireNonNull(b64url, "b64url");
        return B64URL_DEC.decode(b64url);
    }

}
