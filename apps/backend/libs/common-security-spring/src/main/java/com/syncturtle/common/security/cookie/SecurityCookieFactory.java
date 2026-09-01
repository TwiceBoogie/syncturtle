package com.syncturtle.common.security.cookie;

import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_ACCESS_TOKEN;
import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_ADMIN_SESSION_HANDOFF;
import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_CSRF_TOKEN;
import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_REFRESH_TOKEN;

import java.time.Duration;
import java.util.List;

import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;

import com.syncturtle.common.security.property.SecurityCookieProperties;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SecurityCookieFactory {

    public static final String ROOT_PATH = "/";
    public static final String REFRESH_PATH = "/auth";
    public static final String ADMIN_SESSION_HANDOFF_PATH = "/auth/admin/session";

    private static final String HOST_PREFIX = "__Host-";
    private static final String SECURE_PREFIX = "__Secure-";

    private static final String HOST_ACCESS_COOKIE_NAME = HOST_PREFIX + COOKIE_NAME_ACCESS_TOKEN;
    private static final String HOST_REFRESH_COOKIE_NAME = HOST_PREFIX + COOKIE_NAME_REFRESH_TOKEN;
    private static final String SECURE_REFRESH_COOKIE_NAME = SECURE_PREFIX + COOKIE_NAME_REFRESH_TOKEN;
    private static final String HOST_CSRF_COOKIE_NAME = HOST_PREFIX + COOKIE_NAME_CSRF_TOKEN;

    private final SecurityCookieProperties properties;

    public String accessCookieName() {
        return properties.shouldUseManagedPrefix()
                ? HOST_ACCESS_COOKIE_NAME
                : COOKIE_NAME_ACCESS_TOKEN;
    }

    public String refreshCookieName() {
        return properties.shouldUseManagedPrefix()
                ? SECURE_REFRESH_COOKIE_NAME
                : COOKIE_NAME_REFRESH_TOKEN;
    }

    public String csrfCookieName() {
        return properties.shouldUseManagedPrefix()
                ? HOST_CSRF_COOKIE_NAME
                : COOKIE_NAME_CSRF_TOKEN;
    }

    public String adminSessionHandoffCookieName() {
        if (properties.isSecure()) {
            return SECURE_PREFIX + COOKIE_NAME_ADMIN_SESSION_HANDOFF;
        }
        return COOKIE_NAME_ADMIN_SESSION_HANDOFF;
    }

    /**
     * Temporary ST-AUTH-024 migration set. Remove after the coordinated cookie
     * cutover and ST-AUTH-029 reset have completed.
     */
    public List<String> acceptedRefreshCookieNames() {
        return List.of(COOKIE_NAME_REFRESH_TOKEN, HOST_REFRESH_COOKIE_NAME, SECURE_REFRESH_COOKIE_NAME);
    }

    public ResponseCookie accessTokenCookie(String token, Duration maxAge) {
        return createCookie(
                accessCookieName(),
                token,
                ROOT_PATH,
                properties.isSecure(),
                properties.getAccessSameSite(),
                normalizeMaxAge(maxAge));
    }

    public ResponseCookie refreshTokenCookie(String token, Duration maxAge) {
        return createCookie(
                refreshCookieName(),
                token,
                REFRESH_PATH,
                properties.isSecure(),
                properties.getRefreshSameSite(),
                normalizeMaxAge(maxAge));
    }

    public ResponseCookie csrfCookie(String signedToken) {
        return csrfCookie(signedToken, properties.getCsrfMaxAge());
    }

    public ResponseCookie csrfCookie(String signedToken, Duration maxAge) {
        return createCookie(
                csrfCookieName(),
                signedToken,
                ROOT_PATH,
                properties.isSecure(),
                properties.getCsrfSameSite(),
                normalizeMaxAge(maxAge));
    }

    public ResponseCookie adminSessionHandoffCookie(String completionCode, Duration maxAge) {
        Assert.hasText(completionCode, "completionCode is required");

        return createCookie(
                adminSessionHandoffCookieName(),
                completionCode,
                ADMIN_SESSION_HANDOFF_PATH,
                properties.isSecure(),
                "Strict",
                normalizeMaxAge(maxAge));
    }

    public List<ResponseCookie> legacyAccessTokenCookies() {
        return clearAllExcept(clearAccessTokenCookies(), accessCookieName(), ROOT_PATH);
    }

    /**
     * Temporary ST-AUTH-024 migration deletions. The current target identity is
     * excluded so a response can delete legacy cookies before setting the target.
     */
    public List<ResponseCookie> legacyRefreshTokenCookies() {
        return clearAllExcept(clearRefreshTokenCookies(), refreshCookieName(), REFRESH_PATH);
    }

    public List<ResponseCookie> legacyCsrfCookies() {
        return clearAllExcept(clearCsrfCookies(), csrfCookieName(), ROOT_PATH);
    }

    public List<ResponseCookie> clearAccessTokenCookies() {
        return List.of(
                clearCookie(COOKIE_NAME_ACCESS_TOKEN, ROOT_PATH, properties.isSecure(), properties.getAccessSameSite()),
                clearCookie(HOST_ACCESS_COOKIE_NAME, ROOT_PATH, true, properties.getAccessSameSite()));
    }

    public List<ResponseCookie> clearRefreshTokenCookies() {
        return List.of(
                clearCookie(COOKIE_NAME_REFRESH_TOKEN, ROOT_PATH, properties.isSecure(),
                        properties.getRefreshSameSite()),
                clearCookie(COOKIE_NAME_REFRESH_TOKEN, REFRESH_PATH, properties.isSecure(),
                        properties.getRefreshSameSite()),
                clearCookie(HOST_REFRESH_COOKIE_NAME, ROOT_PATH, true, properties.getRefreshSameSite()),
                clearCookie(SECURE_REFRESH_COOKIE_NAME, ROOT_PATH, true, properties.getRefreshSameSite()),
                clearCookie(SECURE_REFRESH_COOKIE_NAME, REFRESH_PATH, true, properties.getRefreshSameSite()));
    }

    public List<ResponseCookie> clearCsrfCookies() {
        return List.of(
                clearCookie(COOKIE_NAME_CSRF_TOKEN, ROOT_PATH, properties.isSecure(), properties.getCsrfSameSite()),
                clearCookie(HOST_CSRF_COOKIE_NAME, ROOT_PATH, true, properties.getCsrfSameSite()));
    }

    public ResponseCookie clearAdminSessionHandoffCookie() {
        return clearCookie(
                adminSessionHandoffCookieName(),
                ADMIN_SESSION_HANDOFF_PATH,
                properties.isSecure(),
                "Strict");
    }

    private List<ResponseCookie> clearAllExcept(List<ResponseCookie> cookies, String name, String path) {
        return cookies.stream()
                .filter(cookie -> !name.equals(cookie.getName()) || !path.equals(cookie.getPath()))
                .toList();
    }

    private ResponseCookie clearCookie(String name, String path, boolean secure, String sameSite) {
        return createCookie(name, "", path, secure, sameSite, Duration.ZERO);
    }

    private ResponseCookie createCookie(
            String name,
            String value,
            String path,
            boolean secure,
            String sameSite,
            Duration maxAge) {
        return ResponseCookie.from(name, value)
                .path(path)
                .secure(secure)
                .httpOnly(true)
                .sameSite(sameSite)
                .maxAge(maxAge)
                .build();
    }

    private static Duration normalizeMaxAge(Duration maxAge) {
        if (maxAge == null || maxAge.isNegative()) {
            return Duration.ZERO;
        }

        return maxAge;
    }

}
