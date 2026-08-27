package com.syncturtle.platform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.server.authorization.AuthorizationContext;

import reactor.core.publisher.Mono;

class GatewayRouteAuthorizationManagerTest {

    private static final String SESSION_ID = "11111111-1111-1111-1111-111111111111";

    private final GatewayRouteAuthorizationManager manager = new GatewayRouteAuthorizationManager(
            new GatewayRouteSecurityPolicy());

    @Nested
    class AuthorizeTests {

        @Test
        @DisplayName("denies anonymous protected request")
        void deniesAnonymousProtectedRequest() {
            // arrange
            AuthorizationContext context = context(HttpMethod.GET, "/api/users/me");
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
            TestingAuthenticationToken authentication = userAuthentication();
            AuthorizationContext context = context(HttpMethod.GET, "/api/users/me");
            // conditions
            // act
            AuthorizationResult result = manager.authorize(Mono.just(authentication), context).block();
            // assert
            assertThat(result).isNotNull();
            assertThat(result.isGranted()).isTrue();
            // verify
        }

        @Test
        @DisplayName("requires authentication for every exact session management route")
        void requiresAuthenticationForEveryExactSessionManagementRoute() {
            // arrange
            List<AuthorizationContext> contexts = List.of(
                    context(HttpMethod.GET, "/api/users/me/sessions"),
                    context(HttpMethod.DELETE, "/api/users/me/sessions"),
                    context(HttpMethod.DELETE, "/api/users/me/sessions/others"),
                    context(HttpMethod.DELETE, "/api/users/me/sessions/" + SESSION_ID));
            // conditions
            // act + assert
            for (AuthorizationContext context : contexts) {
                AuthorizationResult anonymous = manager.authorize(Mono.empty(), context).block();
                AuthorizationResult authenticated = manager.authorize(Mono.just(userAuthentication()), context)
                        .block();
                assertThat(anonymous.isGranted()).isFalse();
                assertThat(authenticated.isGranted()).isTrue();
            }
            // verify
        }

        @Test
        @DisplayName("denies instance route without admin authority")
        void deniesInstanceRouteWithoutAdminAuthority() {
            // arrange
            TestingAuthenticationToken authentication = userAuthentication();
            AuthorizationContext context = context(HttpMethod.GET, "/api/instances/configurations");
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
            // arrange
            TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                    "admin",
                    null,
                    "ROLE_USER",
                    "ROLE_INSTANCE_ADMIN");
            AuthorizationContext context = context(HttpMethod.GET, "/api/instances/admins/me");
            // conditions
            // act
            AuthorizationResult result = manager.authorize(Mono.just(authentication), context).block();
            // assert
            assertThat(result).isNotNull();
            assertThat(result.isGranted()).isTrue();
            // verify
        }

    }

    private static TestingAuthenticationToken userAuthentication() {
        return new TestingAuthenticationToken("user", null, "ROLE_USER");
    }

    private static AuthorizationContext context(HttpMethod method, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(method, path).build());
        return new AuthorizationContext(exchange);
    }

}
