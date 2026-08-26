package com.syncturtle.common.security.cookie;

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
        cookieFactory.legacyCsrfCookies().forEach(response::addCookie);
        response.addCookie(cookieFactory.csrfCookie(signedToken));
    }

    public void clearCsrfCookie(ServerHttpResponse response) {
        cookieFactory.clearCsrfCookies().forEach(response::addCookie);
    }

}
