package com.syncturtle.common.security.cookie;

import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_ACCESS_TOKEN;
import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_CSRF_TOKEN;
import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_REFRESH_TOKEN;

import java.time.Duration;
import java.util.List;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;

import com.syncturtle.common.security.properties.SecurityCookieProperties;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SecurityCookieFactory {

    private static final String HOST_PREFIX = "__Host-";

    private final SecurityCookieProperties properties;

    public String accessCookieName() {
        return realCookieName(COOKIE_NAME_ACCESS_TOKEN);
    }

    public String refreshCookieName() {
        return realCookieName(COOKIE_NAME_REFRESH_TOKEN);
    }

    public String csrfCookieName() {
        return realCookieName(COOKIE_NAME_CSRF_TOKEN);
    }

    public ResponseCookie accessTokenCookie(String token, Duration maxAge) {
        return baseCookie(COOKIE_NAME_ACCESS_TOKEN, token)
                .httpOnly(true)
                .sameSite(properties.getAccessSameSite())
                .maxAge(normalizeMaxAge(maxAge))
                .build();
    }

    public ResponseCookie refreshTokenCookie(String token, Duration maxAge) {
        return baseCookie(COOKIE_NAME_REFRESH_TOKEN, token)
                .httpOnly(true)
                .sameSite(properties.getRefreshSameSite())
                .maxAge(normalizeMaxAge(maxAge))
                .build();
    }

    public ResponseCookie csrfCookie(String signedToken) {
        return baseCookie(COOKIE_NAME_CSRF_TOKEN, signedToken)
                .httpOnly(true)
                .sameSite(properties.getCsrfSameSite())
                .maxAge(properties.getCsrfMaxAge())
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return clearCookie(COOKIE_NAME_ACCESS_TOKEN, true, properties.getAccessSameSite());
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return clearCookie(COOKIE_NAME_REFRESH_TOKEN, true, properties.getRefreshSameSite());
    }

    public ResponseCookie clearCsrfCookie() {
        return clearCookie(COOKIE_NAME_CSRF_TOKEN, true, properties.getCsrfSameSite());
    }

    public List<ResponseCookie> clearAuthCookies() {
        return List.of(
                clearAccessTokenCookie(),
                clearRefreshTokenCookie());
    }

    public List<ResponseCookie> clearAllSecurityCookies() {
        return List.of(
                clearAccessTokenCookie(),
                clearRefreshTokenCookie(),
                clearCsrfCookie());
    }

    private ResponseCookie clearCookie(String cookieName, boolean httpOnly, String sameSite) {
        return baseCookie(cookieName, "")
                .httpOnly(httpOnly)
                .sameSite(sameSite)
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookieBuilder baseCookie(String cookieName, String value) {
        ResponseCookieBuilder builder = ResponseCookie
                .from(realCookieName(cookieName), value)
                .path(properties.getPath())
                .secure(properties.isSecure());

        if (properties.hasDomain()) {
            builder.domain(properties.getDomain());
        }

        return builder;
    }

    private String realCookieName(String cookieName) {
        if (properties.shouldUseHostPrefix()) {
            return HOST_PREFIX + cookieName;
        }

        return cookieName;
    }

    private static Duration normalizeMaxAge(Duration maxAge) {
        if (maxAge == null || maxAge.isNegative()) {
            return Duration.ZERO;
        }

        return maxAge;
    }

}
