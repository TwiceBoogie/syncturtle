package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_ADMIN_SESSION_VERSION;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_INSTANCE_ID;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_ROLES;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_SESSION_ID;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.server.context.SecurityContextServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.platform.gateway.security.PassportAuthenticationToken;
import com.syncturtle.platform.gateway.security.session.ParsedPassportSession;

import reactor.core.publisher.Mono;

@DisplayName("AuthenticationPassportHeadersFilter")
class AuthenticatedPassportHeadersFilterTest {

    @Test
    @DisplayName("replaces forged headers only from validated session")
    void replacesForgedHeadersOnlyFromValidatedSession() {
        ParsedPassportSession session = new ParsedPassportSession(
                2,
                "3a428175-bef1-4c13-ae53-997b6fbfc508",
                "c12c7988-b9fe-4299-a72f-e430d004d37b",
                "91ff9854-e8ac-4bf5-91d8-3117dcd8d05c",
                List.of("INSTANCE_ADMIN", "USER"),
                4L,
                8L,
                true,
                Instant.parse("2026-08-10T11:59:00Z"),
                Instant.parse("2026-08-10T13:00:00Z"));
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://api.syncturtle.test/auth")
                .subject(session.getUserId())
                .issuedAt(Instant.parse("2026-08-10T11:59:00Z"))
                .expiresAt(Instant.parse("2026-08-10T12:15:00Z"))
                .build();
        PassportAuthenticationToken authentication = new PassportAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority("ROLE_INSTANCE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_USER")),
                session);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/instances/admins/me")
                .header(HDR_AUTH_USER_ID, "forged-user")
                .header(HDR_AUTH_SESSION_ID, "forged-session")
                .header(HDR_AUTH_ROLES, "FORGED")
                .build();
        ServerWebExchange exchange = new SecurityContextServerWebExchange(
                MockServerWebExchange.from(request),
                Mono.just(new SecurityContextImpl(authentication)));
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = current -> {
            forwarded.set(current.getRequest());
            return Mono.empty();
        };

        new AuthenticatedPassportHeadersFilter().filter(exchange, chain).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getHeaders().getFirst(HDR_AUTH_USER_ID)).isEqualTo(session.getUserId());
        assertThat(forwarded.get().getHeaders().getFirst(HDR_AUTH_SESSION_ID)).isEqualTo(session.getSessionId());
        assertThat(forwarded.get().getHeaders().getFirst(HDR_AUTH_INSTANCE_ID)).isEqualTo(session.getInstanceId());
        assertThat(forwarded.get().getHeaders().getFirst(HDR_AUTH_ROLES)).isEqualTo("INSTANCE_ADMIN,USER");
        assertThat(forwarded.get().getHeaders().getFirst(HDR_AUTH_ADMIN_SESSION_VERSION)).isEqualTo("8");
    }

    @Test
    @DisplayName("leaves no trusted identity headers when authentication is absent")
    void leavesNoTrustedIdentityHeadersWhenAuthenticationIsAbsent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                .header(HDR_AUTH_USER_ID, "forged-user")
                .header(HDR_AUTH_SESSION_ID, "forged-session")
                .header(HDR_AUTH_ROLES, "FORGED")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
        GatewayFilterChain terminal = current -> {
            forwarded.set(current.getRequest());
            return Mono.empty();
        };
        AuthenticatedPassportHeadersFilter authenticationHeaders = new AuthenticatedPassportHeadersFilter();
        StripInboundAuthHeadersFilter sanitizer = new StripInboundAuthHeadersFilter();
        GatewayFilterChain afterSanitizer = current -> authenticationHeaders.filter(current, terminal);

        sanitizer.filter(exchange, afterSanitizer).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getHeaders().getFirst(HDR_AUTH_USER_ID)).isNull();
        assertThat(forwarded.get().getHeaders().getFirst(HDR_AUTH_SESSION_ID)).isNull();
        assertThat(forwarded.get().getHeaders().getFirst(HDR_AUTH_ROLES)).isNull();
    }
}
