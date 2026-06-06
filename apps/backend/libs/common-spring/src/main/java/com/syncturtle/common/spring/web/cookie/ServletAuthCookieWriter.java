package com.syncturtle.common.spring.web.cookie;

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

    public void setAccessTokenCookie(HttpServletResponse response, String token, Duration maxAge) {
        addCookie(response, cookieFactory.accessTokenCookie(token, maxAge));
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token, Duration maxAge) {
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

    public void clearAccessTokenCookie(HttpServletResponse response) {
        addCookie(response, cookieFactory.clearAccessTokenCookie());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        addCookie(response, cookieFactory.clearRefreshTokenCookie());
    }

    public void clearCsrfCookie(HttpServletResponse response) {
        addCookie(response, cookieFactory.clearCsrfCookie());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        cookieFactory.clearAuthCookies().forEach(cookie -> addCookie(response, cookie));
    }

    public void clearAllSecurityCookies(HttpServletResponse response) {
        cookieFactory.clearAllSecurityCookies().forEach(cookie -> addCookie(response, cookie));
    }

    private void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
