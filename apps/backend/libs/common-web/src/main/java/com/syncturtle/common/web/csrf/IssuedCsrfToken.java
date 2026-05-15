package com.syncturtle.common.web.csrf;

import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
public final class IssuedCsrfToken {

    private final String rawToken;
    private final String signedToken;

    public IssuedCsrfToken(String rawToken, String signedToken) {
        this.rawToken = requireText(rawToken, "rawToken");
        this.signedToken = requireText(signedToken, "signedToken");
    }

    private static String requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value.trim();
    }

}
