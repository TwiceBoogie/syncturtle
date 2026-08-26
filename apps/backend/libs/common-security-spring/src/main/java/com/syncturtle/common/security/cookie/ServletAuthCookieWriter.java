package com.syncturtle.common.security.cookie;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ServletAuthCookieWriter {

    private final SecurityCookieFactory cookieFactory;

    public String accessCookieName() {
        return cookieFactory.accessCookieName();
    }

    public String refreshCookieName() {
        return cookieFactory.refreshCookieName();
    }

    public String csrfCookieName() {
        return cookieFactory.csrfCookieName();
    }

    public String adminSessionHandoffCookieName() {
        return cookieFactory.adminSessionHandoffCookieName();
    }

    public void setAccessTokenCookie(HttpServletResponse response, String token, Duration maxAge) {
        cookieFactory.legacyAccessTokenCookies().forEach(cookie -> addCookie(response, cookie));
        addCookie(response, cookieFactory.accessTokenCookie(token, maxAge));
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token, Duration maxAge) {
        cookieFactory.legacyRefreshTokenCookies().forEach(cookie -> addCookie(response, cookie));
        addCookie(response, cookieFactory.refreshTokenCookie(token, maxAge));
    }

    public void setAuthCookies(
            HttpServletResponse response,
            String accessToken,
            Duration accessMaxAge,
            String refreshToken,
            Duration refreshMaxAge) {
        setAccessTokenCookie(response, accessToken, accessMaxAge);
        setRefreshTokenCookie(response, refreshToken, refreshMaxAge);
    }

    public void setAdminSessionHandoffCookie(HttpServletResponse response, String completionCode, Duration maxAge) {
        addCookie(response, cookieFactory.adminSessionHandoffCookie(completionCode, maxAge));
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {
        cookieFactory.clearAccessTokenCookies().forEach(cookie -> addCookie(response, cookie));
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        cookieFactory.clearRefreshTokenCookies().forEach(cookie -> addCookie(response, cookie));
    }

    public void clearCsrfCookie(HttpServletResponse response) {
        cookieFactory.clearCsrfCookies().forEach(cookie -> addCookie(response, cookie));
    }

    public void clearAdminSessionHandoffCookie(HttpServletResponse response) {
        addCookie(response, cookieFactory.clearAdminSessionHandoffCookie());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
    }

    public void clearAllSecurityCookies(HttpServletResponse response) {
        clearAuthCookies(response);
        clearCsrfCookie(response);
    }

    private void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
