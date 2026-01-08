package com.syncturtle.platform.infra.gateway.filters.factory;

import java.time.Duration;

import org.springframework.http.ResponseCookie;

import com.syncturtle.platform.infra.gateway.configurations.properties.SessionCookieProperties;

final class SessionCookies {

    private SessionCookies() {
    }

    static ResponseCookie build(SessionCookieProperties props, String name, String sessionId, Duration ttl) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, sessionId)
                .httpOnly(props.isHttpOnly())
                .secure(props.isSecure())
                .sameSite(props.getSameSite())
                .path(props.getPath())
                .maxAge(ttl);

        if (props.getDomain() != null && !props.getDomain().isBlank()) {
            b.domain(props.getDomain());
        }
        return b.build();
    }

    static ResponseCookie clear(SessionCookieProperties props, String name) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, "delete")
                .httpOnly(props.isHttpOnly())
                .secure(props.isSecure())
                .sameSite(props.getSameSite())
                .path(props.getPath())
                .maxAge(Duration.ZERO);

        if (props.getDomain() != null && !props.getDomain().isBlank()) {
            b.domain(props.getDomain());
        }
        return b.build();
    }

}
