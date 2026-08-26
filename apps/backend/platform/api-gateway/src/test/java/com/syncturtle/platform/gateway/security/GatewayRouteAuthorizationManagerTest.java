package com.syncturtle.platform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.server.authorization.AuthorizationContext;

import reactor.core.publisher.Mono;

class GatewayRouteAuthorizationManagerTest {

    private final GatewayRouteAuthorizationManager manager = new GatewayRouteAuthorizationManager(
            new GatewayRouteSecurityPolicy());

    @Nested
    class AuthorizeTests {

        @Test
        @DisplayName("denies anonymous protected request")
        void deniesAnonymousProtectedRequest() {
            // arrange
            AuthorizationContext context = context("/api/users/me");
            // conditions
            // act
            AuthorizationResult result = manager.authorize(Mono.empty(), context).block();
            // assert
            assertThat(result).isNotNull();
            assertThat(result.isGranted()).isFalse();
            // verify
        }

        @Test
        @DisplayName("permits authenticated user route")
        void permitsAuthenticatedUserRoute() {
            // arrange
            TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                    "user",
                    null,
                    "ROLE_USER");
            AuthorizationContext context = context("/api/users/me");
            // conditions
            // act
            AuthorizationResult result = manager.authorize(Mono.just(authentication), context).block();
            // assert
            assertThat(result).isNotNull();
            assertThat(result.isGranted()).isTrue();
            // verify
        }

        @Test
        @DisplayName("denies instance route without admin authority")
        void deniesInstanceRouteWithoutAdminAuthority() {
            // arrange
            TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                    "user",
                    null,
                    "ROLE_USER");
            AuthorizationContext context = context("/api/instances/configurations");
            // conditions
            // act
            AuthorizationResult result = manager.authorize(Mono.just(authentication), context).block();
            // assert
            assertThat(result).isNotNull();
            assertThat(result.isGranted()).isFalse();
            // verify
        }

        @Test
        @DisplayName("permits instance route with admin authority")
        void permitsInstanceRouteWithAdminAuthority() {
            TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                    "admin",
                    null,
                    "ROLE_USER",
                    "ROLE_INSTANCE_ADMIN");
            AuthorizationContext context = context("/api/instances/admins/me");

            AuthorizationResult result = manager.authorize(Mono.just(authentication), context).block();

            assertThat(result).isNotNull();
            assertThat(result.isGranted()).isTrue();
        }

    }

    private static AuthorizationContext context(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(path).build());
        return new AuthorizationContext(exchange);
    }

}
