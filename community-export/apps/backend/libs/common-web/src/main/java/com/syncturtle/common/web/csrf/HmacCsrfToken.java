package com.syncturtle.common.web.csrf;

import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacCsrfToken {

    private final String hmacAlgorithm;
    private final String secretKey;

    public HmacCsrfToken(String hmacAlgorithm, String secretKey) {
        this.hmacAlgorithm = hmacAlgorithm;
        this.secretKey = secretKey;
    }

    public String generateSignedToken(String token) {
        try {
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(new SecretKeySpec(secretKey.getBytes(), hmacAlgorithm));
            byte[] hmac = mac.doFinal(token.getBytes());
            return token + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign token", e);
        }
    }

    public boolean isValid(String tokenWithSignature) {
        try {
            String[] parts = tokenWithSignature.split("\\.");
            if (parts.length != 2) {
                return false;
            }
            String token = parts[0];
            String expected = generateSignedToken(token);
            return expected.equals(tokenWithSignature);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

}
