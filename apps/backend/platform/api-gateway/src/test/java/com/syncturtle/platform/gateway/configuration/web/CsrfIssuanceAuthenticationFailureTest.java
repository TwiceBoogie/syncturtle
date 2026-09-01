package com.syncturtle.platform.gateway.configuration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.property.SecurityCookieProperties;
import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.security.PassportAuthenticationMetrics;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

class CsrfIssuanceAuthenticationFailureTest {

    private GatewaySecurityConfiguration configuration;
    private SecurityCookieFactory cookieFactory;

    @BeforeEach
    void setUp() {
        cookieFactory = new SecurityCookieFactory(new SecurityCookieProperties(
                false,
                false,
                "Lax",
                "Lax",
                "Lax",
                Duration.ofMinutes(30)));
        configuration = new GatewaySecurityConfiguration(
                new PassportAuthenticationMetrics(new SimpleMeterRegistry()),
                cookieFactory);
    }

    @Nested
    class AuthenticationFailure {

        @ParameterizedTest
        @EnumSource(value = PassportAuthenticationFailureReason.class, names = {
                "CREDENTIAL_INVALID",
                "JWT_REJECTED",
                "SESSION_VERSION_UNSUPPORTED",
                "SESSION_INACTIVE"
        })
        void rejectsPresentedInvalidRevokedAndUnsupportedCredentialsWithoutAnonymousDowngrade(
                PassportAuthenticationFailureReason reason) {
            MockServerWebExchange exchange = csrfIssuanceExchange();

            invokeAuthenticationFailure(exchange, new PassportAuthenticationException(reason));

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(exchange.getResponse().getCookies()).containsKeys(
                    cookieFactory.accessCookieName(),
                    cookieFactory.refreshCookieName(),
                    cookieFactory.csrfCookieName());
        }

        @Test
        void mapsOperationalAuthenticationFailureToServiceUnavailableWithoutClearingCredentials() {
            MockServerWebExchange exchange = csrfIssuanceExchange();

            invokeAuthenticationFailure(
                    exchange,
                    new PassportAuthenticationException(PassportAuthenticationFailureReason.REDIS_UNAVAILABLE));

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(exchange.getResponse().getCookies()).isEmpty();
        }
    }

    private void invokeAuthenticationFailure(
            MockServerWebExchange exchange,
            PassportAuthenticationException exception) {
        Mono<Void> result = ReflectionTestUtils.invokeMethod(
                configuration,
                "authenticationFailure",
                exchange,
                exception);
        assertThat(result).isNotNull();
        result.block();
    }

    private static MockServerWebExchange csrfIssuanceExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/get-csrf-token").build());
    }
}
