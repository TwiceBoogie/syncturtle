package com.syncturtle.platform.gateway.security.csrf;

import java.time.Duration;

import org.springframework.http.server.reactive.ServerHttpResponse;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GatewayCsrfCookieWriter {

    private final SecurityCookieFactory cookieFactory;

    public void setCsrfCookie(ServerHttpResponse response, String signedToken, Duration maxAge) {
        cookieFactory.legacyCsrfCookies().forEach(response::addCookie);
        response.addCookie(cookieFactory.csrfCookie(signedToken, maxAge));
    }

}
