package com.syncturtle.platform.gateway.filter.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.property.SecurityCookieProperties;
import com.syncturtle.platform.gateway.security.GatewayRouteSecurityPolicy;
import com.syncturtle.platform.gateway.security.browser.GatewayBrowserOriginPolicy;

import reactor.core.publisher.Mono;

class BrowserRequestPolicyFilterTest {

    private BrowserRequestPolicyFilter filter;
    private SecurityCookieFactory cookies;
    private SecureTokenGenerator tokens;

    @BeforeEach
    void setUp() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("https://web.example.test", "https://admin.example.test"));
        cors.setAllowCredentials(true);
        GlobalCorsProperties properties = new GlobalCorsProperties();
        properties.getCorsConfigurations().put("/**", cors);
        cookies = new SecurityCookieFactory(new SecurityCookieProperties(true, true, "Lax", "Lax", "Lax"));
        tokens = mock(SecureTokenGenerator.class);
        filter = new BrowserRequestPolicyFilter(new GatewayBrowserOriginPolicy(properties),
                new GatewayRouteSecurityPolicy(), cookies, tokens);
    }

    @Test
    void rejectsMissingNullAndUntrustedOriginsOnRefreshAndLogout() {
        for (String path : List.of("/auth/sign-in", "/auth/refresh", "/auth/sign-out")) {
            for (String origin : List.of("", "null", "https://evil.example.test")) {
                var request = MockServerHttpRequest.post(path)
                        .header(GatewayHeaders.HDR_BROWSER_REQUEST, "1");
                if (!origin.isEmpty()) request.header(HttpHeaders.ORIGIN, origin);
                MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
                filter.filter(exchange, unused -> Mono.empty()).block();
                assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            }
        }
    }

    @Test
    void rejectsTrustedOriginWithoutBrowserHeaderForFetchRequests() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/refresh")
                .header(HttpHeaders.ORIGIN, "https://web.example.test").build());
        filter.filter(exchange, unused -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        MockServerWebExchange deletion = MockServerWebExchange.from(MockServerHttpRequest
                .delete("/api/workspaces/example")
                .header(HttpHeaders.ORIGIN, "https://web.example.test").build());
        filter.filter(deletion, unused -> Mono.empty()).block();
        assertThat(deletion.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void originMatchingIsExactAndRejectsMultipleValues() {
        for (String origin : List.of("https://admin.example.test/", "https://ADMIN.example.test",
                "https://admin.example.test.evil.test")) {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/sign-in")
                    .header(HttpHeaders.ORIGIN, origin)
                    .header(GatewayHeaders.HDR_BROWSER_REQUEST, "1").build());
            filter.filter(exchange, unused -> Mono.empty()).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        MockServerWebExchange duplicate = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/sign-in")
                .header(HttpHeaders.ORIGIN, "https://admin.example.test", "https://web.example.test")
                .header(GatewayHeaders.HDR_BROWSER_REQUEST, "1").build());
        filter.filter(duplicate, unused -> Mono.empty()).block();
        assertThat(duplicate.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void nativeAdminFormReceivesIndependentBrowserBindingWithoutCustomHeader() {
        when(tokens.generateBase64Url(32)).thenReturn("browser-binding-secret");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .post("/api/instances/admins/sign-in")
                .header(HttpHeaders.ORIGIN, "https://admin.example.test")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).build());
        AtomicReference<String> binding = new AtomicReference<>();
        GatewayFilterChain chain = current -> {
            binding.set(current.getRequest().getHeaders().getFirst(GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING));
            return Mono.empty();
        };
        filter.filter(exchange, chain).block();
        assertThat(binding.get()).matches("[0-9a-f]{64}");
        assertThat(exchange.getResponse().getCookies()).containsKey(cookies.adminHandoffBindingCookieName());
        assertThat(exchange.getResponse().getCookies().getFirst(cookies.adminHandoffBindingCookieName()).isHttpOnly())
                .isTrue();
    }

    @Test
    void removesDownstreamSpoofingOfGatewayAuthenticationRejectionMarker() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me").build());
        filter.filter(exchange, current -> {
            current.getResponse().getHeaders().set(GatewayHeaders.HDR_GATEWAY_AUTH_REJECTION, "1");
            return current.getResponse().setComplete();
        }).block();
        assertThat(exchange.getResponse().getHeaders().containsHeader(GatewayHeaders.HDR_GATEWAY_AUTH_REJECTION))
                .isFalse();
    }

    @Test
    void permitsOriginlessBearerOnlyProtectedMutationForApiClients() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .patch("/api/workspaces/example")
                .header(HttpHeaders.AUTHORIZATION, "Bearer service-token")
                .build());
        AtomicReference<Boolean> forwarded = new AtomicReference<>(false);
        filter.filter(exchange, unused -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();
        assertThat(forwarded.get()).isTrue();
    }

    @Test
    void bearerHeaderCannotBypassBrowserPolicyWhenCookiesOrOriginArePresent() {
        for (boolean includeCookie : List.of(false, true)) {
            var request = MockServerHttpRequest.patch("/api/workspaces/example")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer service-token");
            if (includeCookie) {
                request.cookie(new org.springframework.http.HttpCookie(cookies.accessCookieName(), "access"));
            } else {
                request.header(HttpHeaders.ORIGIN, "https://evil.example.test");
            }
            MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
            filter.filter(exchange, unused -> Mono.empty()).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
