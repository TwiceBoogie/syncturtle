package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_INTERNAL_CSRF_SESSION_ID;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.csrf.impl.HmacCsrfTokenSigner;
import com.syncturtle.common.security.property.SecurityCookieProperties;
import com.syncturtle.platform.gateway.configuration.property.GatewayCsrfProperties;
import com.syncturtle.platform.gateway.security.PassportAuthenticationToken;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfRoutePolicy;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenProcessor;
import com.syncturtle.platform.gateway.security.csrf.IssuedPreAuthCsrfToken;
import com.syncturtle.platform.gateway.security.csrf.IssuedSessionCsrfToken;
import com.syncturtle.platform.gateway.security.session.ParsedPassportSession;

import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("CsrfMiddlewareFilter")
class CsrfMiddlewareFilterTest {

    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private static final String SESSION_ID = "33333333-3333-3333-3333-333333333333";
    private static final String OTHER_SESSION_ID = "44444444-4444-4444-4444-444444444444";

    private SecurityCookieFactory cookieFactory;
    private GatewayCsrfTokenProcessor tokenProcessor;
    private CsrfMiddlewareFilter filter;

    @BeforeEach
    void setup() {
        SecurityCookieProperties cookieProperties = new SecurityCookieProperties(false, false, "Lax", "Lax", "Lax",
                Duration.ofMinutes(30));
        cookieFactory = new SecurityCookieFactory(cookieProperties);
        GatewayCsrfProperties csrfProperties = new GatewayCsrfProperties(Duration.ofMinutes(30), Duration.ofDays(7), 32,
                2048, 1024);
        tokenProcessor = new GatewayCsrfTokenProcessor(
                new JsonMapper(),
                new HmacCsrfTokenSigner("k".repeat(32).getBytes(StandardCharsets.UTF_8)),
                new Base64UrlSecureTokenGenerator(),
                csrfProperties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        filter = new CsrfMiddlewareFilter(cookieFactory, tokenProcessor, new GatewayCsrfRoutePolicy());
    }

    @Nested
    class PreAuthTests {

        @Test
        void validatesFormEqualityPreservesBodyAndBuildsExistingHandoffBinding() {
            // arrange
            IssuedPreAuthCsrfToken issued = tokenProcessor.issuePreAuth();
            String body = "csrfmiddlewaretoken=" + issued.getSubmittedToken() + "&email=user%40example.test";
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/instances/admins/sign-in")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .cookie(new HttpCookie(cookieFactory.csrfCookieName(), issued.getSignedCookieToken()))
                    .body(body);
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<String> forwardedBody = new AtomicReference<>();
            AtomicReference<String> binding = new AtomicReference<>();
            GatewayFilterChain chain = current -> {
                binding.set(current.getRequest().getHeaders().getFirst(HDR_PREAUTH_TRANSACTION_BINDING));
                return DataBufferUtils.join(current.getRequest().getBody()).doOnNext(buffer -> {
                    forwardedBody.set(buffer.toString(StandardCharsets.UTF_8));
                    DataBufferUtils.release(buffer);
                }).then();
            };
            // conditions
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(forwardedBody).hasValue(body);
            assertThat(binding.get()).isNotBlank();
            // verify
        }

    }

    @Nested
    class TransportSessionTests {

        @Test
        void reconstructsTrustedSessionHeaderOnlyAfterSessionValidation() {
            // arrange
            IssuedSessionCsrfToken issued = tokenProcessor.issueSession(SESSION_ID, NOW.plus(Duration.ofDays(7)));
            MockServerHttpRequest request = MockServerHttpRequest.post("/auth/refresh")
                    .cookie(new HttpCookie(cookieFactory.csrfCookieName(), issued.getSignedCookieToken()))
                    .header("X-CSRF-Token", issued.getSubmittedToken())
                    .header(HDR_INTERNAL_CSRF_SESSION_ID, OTHER_SESSION_ID)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<String> trustedSessionId = new AtomicReference<>();
            // conditions
            // act
            filter.filter(exchange, current -> {
                trustedSessionId.set(current.getRequest().getHeaders().getFirst(HDR_INTERNAL_CSRF_SESSION_ID));
                return Mono.empty();
            }).block();
            // assert
            assertThat(trustedSessionId).hasValue(SESSION_ID);
            // verify
        }

        @Test
        void rejectsPreAuthOnRefreshAndClearsCsrfIdentity() {
            // arrange
            IssuedPreAuthCsrfToken issued = tokenProcessor.issuePreAuth();
            MockServerHttpRequest request = MockServerHttpRequest.post("/auth/refresh")
                    .cookie(new HttpCookie(cookieFactory.csrfCookieName(), issued.getSignedCookieToken()))
                    .header("X-CSRF-Token", issued.getSubmittedToken())
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicBoolean forwarded = new AtomicBoolean();
            // conditions
            // act
            filter.filter(exchange, current -> {
                forwarded.set(true);
                return Mono.empty();
            }).block();
            // assert
            assertThat(forwarded).isFalse();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(exchange.getResponse().getCookies()).containsKey(cookieFactory.csrfCookieName());
            // verify
        }

    }

    @Nested
    class AuthenticatedSessionTests {

        @Test
        void forwardsMatchingSessionOnceWhenDownstreamCompletesEmpty() {
            // arrange
            IssuedSessionCsrfToken issued = tokenProcessor.issueSession(SESSION_ID, NOW.plus(Duration.ofDays(7)));
            MockServerHttpRequest request = MockServerHttpRequest.patch("/api/users/me/profile")
                    .cookie(new HttpCookie(cookieFactory.csrfCookieName(), issued.getSignedCookieToken()))
                    .header("X-CSRF-Token", issued.getSubmittedToken())
                    .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request).mutate()
                    .principal(Mono.just(authentication(SESSION_ID)))
                    .build();
            AtomicInteger forwarded = new AtomicInteger();
            // conditions
            // act
            filter.filter(exchange, current -> {
                forwarded.incrementAndGet();
                return Mono.empty();
            }).block();
            // assert
            assertThat(forwarded).hasValue(1);
            assertThat(exchange.getResponse().getStatusCode()).isNull();
            assertThat(exchange.getResponse().getCookies()).doesNotContainKey(cookieFactory.csrfCookieName());
            // verify
        }

        @Test
        void doesNotRejectAfterMatchingSessionCommitsSuccessfulResponse() {
            // arrange
            IssuedSessionCsrfToken issued = tokenProcessor.issueSession(SESSION_ID, NOW.plus(Duration.ofDays(7)));
            MockServerHttpRequest request = MockServerHttpRequest.patch("/api/users/me/profile")
                    .cookie(new HttpCookie(cookieFactory.csrfCookieName(), issued.getSignedCookieToken()))
                    .header("X-CSRF-Token", issued.getSubmittedToken())
                    .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request).mutate()
                    .principal(Mono.just(authentication(SESSION_ID)))
                    .build();
            AtomicInteger forwarded = new AtomicInteger();
            // conditions
            // act
            filter.filter(exchange, current -> {
                forwarded.incrementAndGet();
                current.getResponse().setStatusCode(HttpStatus.OK);
                return current.getResponse().setComplete();
            }).block();
            // assert
            assertThat(forwarded).hasValue(1);
            assertThat(exchange.getResponse().isCommitted()).isTrue();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(exchange.getResponse().getCookies()).doesNotContainKey(cookieFactory.csrfCookieName());
            // verify
        }

        @Test
        void requiresTokenSidToMatchValidatedPassportSid() {
            // arrange
            IssuedSessionCsrfToken issued = tokenProcessor.issueSession(SESSION_ID, NOW.plus(Duration.ofDays(7)));
            MockServerHttpRequest request = MockServerHttpRequest.patch("/api/users/me/profile")
                    .cookie(new HttpCookie(cookieFactory.csrfCookieName(), issued.getSignedCookieToken()))
                    .header("X-CSRF-Token", issued.getSubmittedToken())
                    .build();
            MockServerWebExchange baseExchange = MockServerWebExchange.from(request);
            ServerWebExchange exchange = baseExchange.mutate()
                    .principal(Mono.just(authentication(OTHER_SESSION_ID)))
                    .build();
            AtomicBoolean forwarded = new AtomicBoolean();
            // conditions
            // act
            filter.filter(exchange, current -> {
                forwarded.set(true);
                return Mono.empty();
            }).block();
            // assert
            assertThat(forwarded).isFalse();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(exchange.getResponse().getCookies()).containsKey(cookieFactory.csrfCookieName());
            assertThat(baseExchange.getResponse().getBodyAsString().block()).contains("INVALID_CSRF_TOKEN");
            // verify
        }

    }

    private PassportAuthenticationToken authentication(String sessionId) {
        Jwt jwt = Jwt.withTokenValue("access")
                .header("alg", "RS256")
                .subject("11111111-1111-1111-1111-111111111111")
                .issuedAt(NOW.minusSeconds(60))
                .expiresAt(NOW.plusSeconds(600))
                .build();
        ParsedPassportSession session = new ParsedPassportSession(
                2,
                sessionId,
                "11111111-1111-1111-1111-111111111111",
                "22222222-2222-2222-2222-222222222222",
                List.of("USER"),
                1L,
                null,
                true,
                NOW.minusSeconds(60),
                NOW.plus(Duration.ofDays(7)));
        return new PassportAuthenticationToken(jwt, List.of(), session);
    }

}
