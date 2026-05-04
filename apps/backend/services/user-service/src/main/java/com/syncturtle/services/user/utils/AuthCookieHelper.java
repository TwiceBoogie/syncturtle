package com.syncturtle.services.user.utils;

import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_ACCESS_TOKEN;
import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_CSRF_TOKEN;
import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_REFRESH_TOKEN;

import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;
import org.springframework.stereotype.Component;

import com.syncturtle.services.user.configurations.properties.AuthProperties;
import com.syncturtle.services.user.configurations.properties.EndpointProperties;
import com.syncturtle.services.user.configurations.properties.HttpProperties;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthCookieHelper {

    private final AuthProperties authProperties;
    private final EndpointProperties endpointProperties;
    private final HttpProperties httpProperties;

    public boolean isSecure() {
        String webBaseUrl = endpointProperties.getWebBaseUrl();
        String adminBaseUrl = endpointProperties.getAdminBaseUrl();
        String apiBaseUrl = endpointProperties.getApiBaseUrl();

        return webBaseUrl != null
                && adminBaseUrl != null
                && apiBaseUrl != null
                && webBaseUrl.startsWith("https")
                && adminBaseUrl.startsWith("https")
                && apiBaseUrl.startsWith("https");
    }

    public void setAccessTokenCookie(HttpServletResponse response, String token, String sameSite) {
        ResponseCookie cookie = baseCookie(COOKIE_NAME_ACCESS_TOKEN, token)
                .httpOnly(true)
                .sameSite(sameSite)
                .maxAge(authProperties.accessTokenMaxAgeSeconds())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = baseCookie(COOKIE_NAME_REFRESH_TOKEN, token)
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(authProperties.refreshTokenMaxAgeSeconds())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void setCsrfTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = baseCookie(COOKIE_NAME_CSRF_TOKEN, token)
                .httpOnly(false)
                .sameSite("Lax")
                .maxAge(authProperties.accessTokenMaxAgeSeconds())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAccessTokenCookie(HttpServletResponse response, String sameSite) {
        clearCookie(response, COOKIE_NAME_ACCESS_TOKEN, sameSite, true);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        clearCookie(response, COOKIE_NAME_REFRESH_TOKEN, "Lax", true);
    }

    public void clearCsrfTokenCookie(HttpServletResponse response) {
        clearCookie(response, COOKIE_NAME_CSRF_TOKEN, "Lax", false);
    }

    public List<String> buildForceLogoutCookieHeaders() {
        return List.of(
                buildClearCookieHeader(COOKIE_NAME_ACCESS_TOKEN, "Lax", true),
                buildClearCookieHeader(COOKIE_NAME_REFRESH_TOKEN, "Lax", true),
                buildClearCookieHeader(COOKIE_NAME_CSRF_TOKEN, "Lax", false));
    }

    private String cookieDomain() {
        String domain = httpProperties.getCookieDomain();
        if (domain == null) {
            return null;
        }

        domain = domain.trim();
        if (domain.startsWith(".")) {
            domain = domain.substring(1);
        }

        return domain.isBlank() ? null : domain;
    }

    private String realCookieName(String cookieName) {
        if (isSecure() && cookieDomain() == null) {
            return "__Host-" + cookieName;
        }
        return cookieName;
    }

    private ResponseCookieBuilder baseCookie(String cookieName, String value) {
        ResponseCookieBuilder builder = ResponseCookie
                .from(realCookieName(cookieName), value)
                .path("/")
                .secure(isSecure());

        String domain = cookieDomain();
        if (domain != null) {
            builder.domain(domain);
        }

        return builder;
    }

    private void clearCookie(HttpServletResponse response, String cookieName, String sameSite, boolean httpOnly) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildClearCookieHeader(cookieName, sameSite, httpOnly));
    }

    private String buildClearCookieHeader(String cookieName, String sameSite, boolean httpOnly) {
        ResponseCookieBuilder builder = baseCookie(cookieName, "")
                .httpOnly(httpOnly)
                .sameSite(sameSite)
                .maxAge(Duration.ZERO);

        return builder.build().toString();
    }

}
