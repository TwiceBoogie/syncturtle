package com.syncturtle.services.user.service.param;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class CredentialAuthenticationParam {

    private final String email;
    private final String secret;
    private final boolean signup;
    private final String ipAddress;
    private final String userAgent;

    @Builder
    private CredentialAuthenticationParam(
            String email,
            String secret,
            boolean signup,
            String ipAddress,
            String userAgent) {
        Assert.hasText(email, "email is required");

        this.email = email;
        this.secret = secret;
        this.signup = signup;
        this.ipAddress = normalizeNullable(ipAddress);
        this.userAgent = normalizeNullable(userAgent);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
