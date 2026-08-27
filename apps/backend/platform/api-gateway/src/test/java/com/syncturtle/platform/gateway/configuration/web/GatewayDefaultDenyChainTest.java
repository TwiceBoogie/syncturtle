package com.syncturtle.platform.gateway.configuration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;

import com.syncturtle.platform.gateway.security.GatewayRouteSecurityPolicy;
import com.syncturtle.platform.gateway.security.PassportAuthenticationMetrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GatewayDefaultDenyChainTest {

    private WebFilterChainProxy security;

    @BeforeEach
    void setup() {
        GatewaySecurityConfiguration configuration = new GatewaySecurityConfiguration(
                new PassportAuthenticationMetrics(new SimpleMeterRegistry()));
        GatewayRouteSecurityPolicy policy = new GatewayRouteSecurityPolicy();
        SecurityWebFilterChain publicChain = configuration.publicSecurityWebFilterChain(
                ServerHttpSecurity.http(),
                policy);
        SecurityWebFilterChain defaultDenyChain = configuration.defaultDenySecurityWebFilterChain(
                ServerHttpSecurity.http());
        security = new WebFilterChainProxy(publicChain, defaultDenyChain);
    }

    @Nested
    class FilterTests {

        @Test
        @DisplayName("permits knwon public route to reach application handler")
        void permitsKnownPublicRouteToReachApplicationHandler() {
            // arrange
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/get-csrf-token").build());
            AtomicBoolean reachedHandler = new AtomicBoolean();
            // conditions
            // act
            security.filter(exchange, current -> {
                reachedHandler.set(true);
                current.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                return current.getResponse().setComplete();
            }).block();
            // assert
            assertThat(reachedHandler).isTrue();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            // verify
        }

        @Test
        @DisplayName("denies unclassified route before application handler")
        void deniesUnclassifiedRouteBeforeApplicationHandler() {
            // arrange
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/not-classified").build());
            AtomicBoolean reachedHandler = new AtomicBoolean();
            // conditions
            // act
            security.filter(exchange, current -> {
                reachedHandler.set(true);
                return current.getResponse().setComplete();
            }).block();
            // assert
            assertThat(reachedHandler).isFalse();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            // verify
        }

        @Test
        @DisplayName("denies invalid session-management shapes before application handler")
        void deniesInvalidSessionManagementShapesBeforeApplicationHandler() {
            // arrange
            List<MockServerWebExchange> exchanges = List.of(
                    MockServerWebExchange.from(
                            MockServerHttpRequest.delete("/api/users/me/sessions/not-a-uuid").build()),
                    MockServerWebExchange.from(
                            MockServerHttpRequest.post("/api/users/me/sessions").build()));
            // conditions
            // act + assert
            for (MockServerWebExchange exchange : exchanges) {
                AtomicBoolean reachedHandler = new AtomicBoolean();
                security.filter(exchange, current -> {
                    reachedHandler.set(true);
                    return current.getResponse().setComplete();
                }).block();

                assertThat(reachedHandler).isFalse();
                assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            }
            // verify
        }

    }

}
