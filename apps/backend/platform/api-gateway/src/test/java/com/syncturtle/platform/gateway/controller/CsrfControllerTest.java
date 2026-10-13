package com.syncturtle.platform.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.security.cookie.ReactiveCsrfCookieWriter;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.csrf.impl.HmacCsrfTokenSigner;
import com.syncturtle.common.security.property.SecurityCookieProperties;
import com.syncturtle.platform.gateway.configuration.property.GatewayCsrfProperties;
import com.syncturtle.platform.gateway.dto.response.CsrfTokenResponse;
import com.syncturtle.platform.gateway.security.PassportAuthenticationToken;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenProcessor;
import com.syncturtle.platform.gateway.security.session.ParsedPassportSession;
import com.syncturtle.platform.gateway.type.GatewayCsrfScope;

import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

class CsrfControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private static final String SESSION_ID = "33333333-3333-3333-3333-333333333333";

    private SecurityCookieFactory cookieFactory;
    private CsrfController controller;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        cookieFactory = new SecurityCookieFactory(new SecurityCookieProperties(
                false, false, "Lax", "Lax", "Lax", Duration.ofMinutes(30)));
        GatewayCsrfTokenProcessor processor = new GatewayCsrfTokenProcessor(new JsonMapper(),
                new HmacCsrfTokenSigner("k".repeat(32).getBytes(StandardCharsets.UTF_8)),
                new Base64UrlSecureTokenGenerator(),
                new GatewayCsrfProperties(Duration.ofMinutes(30), Duration.ofDays(7), 32, 2048, 1024), clock);
        controller = new CsrfController(processor, new ReactiveCsrfCookieWriter(cookieFactory), clock);
    }

    @Nested
    class GetCsrfTokenTests {

        @Test
        void issuesPreAuthWhenOptionalAuthenticationHasNoCredential() {
            // arrange
            MockServerWebExchange exchange = MockServerWebExchange
                    .from(MockServerHttpRequest.get("/api/get-csrf-token").build());
            // conditions
            // act
            ResponseEntity<CsrfTokenResponse> result = controller.getCsrfToken(exchange).block();
            // assert
            assertThat(result).isNotNull();
            assertThat(result.getBody().getScope()).isEqualTo(GatewayCsrfScope.PREAUTH);
            assertThat(result.getBody().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
            assertThat(result.getBody().getCsrfToken()).isNotBlank();
            assertThat(exchange.getResponse().getCookies()).containsKey(cookieFactory.csrfCookieName());
            assertThat(result.getHeaders().getCacheControl()).isEqualTo("no-store");
            // verify
        }

        @Test
        void issuesSessionFromTheValidatedPassportFamily() {
            Instant familyIdleExpiry = NOW.plus(Duration.ofHours(8));
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/get-csrf-token").build();
            ServerWebExchange exchange = MockServerWebExchange.from(request)
                    .mutate()
                    .principal(Mono.just(authentication(familyIdleExpiry)))
                    .build();

            ResponseEntity<CsrfTokenResponse> result = controller.getCsrfToken(exchange).block();

            assertThat(result).isNotNull();
            assertThat(result.getBody().getScope()).isEqualTo(GatewayCsrfScope.SESSION);
            assertThat(result.getBody().getExpiresAt()).isEqualTo(familyIdleExpiry);
            assertThat(result.getBody().getCsrfToken()).doesNotContain(SESSION_ID);
        }

    }

    private PassportAuthenticationToken authentication(Instant familyIdleExpiry) {
        Jwt jwt = Jwt.withTokenValue("access")
                .header("alg", "RS256")
                .subject("11111111-1111-1111-1111-111111111111")
                .issuedAt(NOW.minusSeconds(60))
                .expiresAt(NOW.plusSeconds(600))
                .build();
        ParsedPassportSession session = new ParsedPassportSession(
                2,
                SESSION_ID,
                "11111111-1111-1111-1111-111111111111",
                "22222222-2222-2222-2222-222222222222",
                List.of("USER"),
                1L,
                null,
                true,
                NOW.minusSeconds(60),
                familyIdleExpiry);
        return new PassportAuthenticationToken(jwt, List.of(), session);
    }

}
