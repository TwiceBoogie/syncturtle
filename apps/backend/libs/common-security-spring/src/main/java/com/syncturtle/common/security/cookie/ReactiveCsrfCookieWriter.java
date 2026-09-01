package com.syncturtle.common.security.cookie;

import java.time.Duration;

import org.springframework.http.server.reactive.ServerHttpResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ReactiveCsrfCookieWriter {

    private final SecurityCookieFactory cookieFactory;

    public String accessCookieName() {
        return cookieFactory.accessCookieName();
    }

    public String csrfCookieName() {
        return cookieFactory.csrfCookieName();
    }

    public void setCsrfCookie(ServerHttpResponse response, String signedToken) {
        setCsrfCookie(response, signedToken, null);
    }

    public void setCsrfCookie(ServerHttpResponse response, String signedToken, Duration maxAge) {
        cookieFactory.legacyCsrfCookies().forEach(response::addCookie);
        response.addCookie(maxAge == null
                ? cookieFactory.csrfCookie(signedToken)
                : cookieFactory.csrfCookie(signedToken, maxAge));
    }

    public void clearCsrfCookie(ServerHttpResponse response) {
        cookieFactory.clearCsrfCookies().forEach(response::addCookie);
    }

}
