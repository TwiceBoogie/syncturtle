package com.syncturtle.platform.gateway.configuration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

import com.syncturtle.platform.gateway.security.PassportAuthenticationMetrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

class GatewayCorsConfigurationTest {

    private CorsWebFilter corsWebFilter;

    @BeforeEach
    void setUp() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(List.of("http://localhost:3000"));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setAllowCredentials(true);

        GlobalCorsProperties properties = new GlobalCorsProperties();
        properties.getCorsConfigurations().put("/**", corsConfiguration);

        GatewaySecurityConfiguration securityConfiguration = new GatewaySecurityConfiguration(
                new PassportAuthenticationMetrics(new SimpleMeterRegistry()));
        CorsConfigurationSource source = securityConfiguration.gatewayCorsConfigurationSource(properties);
        corsWebFilter = new CorsWebFilter(source);
    }

    @Nested
    class SecurityResponses {

        @Test
        void addsConfiguredCorsHeadersWhenSecurityReturnsUnauthorized() {
            MockServerWebExchange exchange = exchangeFrom("http://localhost:3000");

            corsWebFilter.filter(exchange, ignored -> {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }).block();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin())
                    .isEqualTo("http://localhost:3000");
            assertThat(exchange.getResponse().getHeaders().getAccessControlAllowCredentials()).isTrue();
        }

        @Test
        void rejectsAnUnconfiguredOriginBeforeInvokingTheApplicationChain() {
            MockServerWebExchange exchange = exchangeFrom("http://untrusted.example");
            AtomicBoolean chainInvoked = new AtomicBoolean(false);

            corsWebFilter.filter(exchange, ignored -> {
                chainInvoked.set(true);
                return Mono.empty();
            }).block();

            assertThat(chainInvoked).isFalse();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin()).isNull();
        }
    }

    private static MockServerWebExchange exchangeFrom(String origin) {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "http://localhost:8000/api/users/me")
                .header(HttpHeaders.ORIGIN, origin)
                .build();
        return MockServerWebExchange.from(request);
    }
}
