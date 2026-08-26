package com.syncturtle.platform.gateway.filter.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.csrf.CsrfTokenService;
import com.syncturtle.common.security.property.SecurityCookieProperties;
import com.syncturtle.platform.gateway.filter.GatewayFilterOrders;
import com.syncturtle.platform.gateway.security.BrowserSecurityCookiePolicy;
import com.syncturtle.platform.gateway.security.CookieOrBearerServerAuthenticationConverter;

import reactor.core.publisher.Mono;

@DisplayName("BrowserSecurityCookieIsolationFilter")
class BrowserSecurityCookieIsolationFilterTest {

    private SecurityCookieFactory cookieFactory;
    private BrowserSecurityCookieIsolationFilter filter;

    @BeforeEach
    void setup() {
        Duration duration = Duration.ofMinutes(30);
        SecurityCookieProperties properties = new SecurityCookieProperties(false, false, "Lax", "Lax", "Lax", duration);
        cookieFactory = new SecurityCookieFactory(properties);
        BrowserSecurityCookiePolicy policy = new BrowserSecurityCookiePolicy(cookieFactory);

        filter = new BrowserSecurityCookieIsolationFilter(policy, cookieFactory);
    }

    @Nested
    @DisplayName("filter(ServerWebExchange, GatewayFilterChain)")
    class FilterTests {

        @Test
        @DisplayName("strips access authorization csrf header and unknown cookies")
        void stripsAccessAuthorizationCsrfHeaderAndUnknownCookies() {
            // arrange
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer access")
                    .header("X-CSRF-Token", "raw-csrf")
                    .cookie(
                            new HttpCookie("access_token", "access"),
                            new HttpCookie("csrf_token", "signed-csrf"),
                            new HttpCookie("theme", "dark"))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<HttpHeaders> downstreamHeaders = new AtomicReference<>();
            GatewayFilterChain chain = current -> {
                downstreamHeaders.set(current.getRequest().getHeaders());
                return Mono.empty();
            };
            // conditions
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(downstreamHeaders.get().containsHeader(HttpHeaders.COOKIE)).isFalse();
            assertThat(downstreamHeaders.get().containsHeader(HttpHeaders.AUTHORIZATION)).isFalse();
            assertThat(downstreamHeaders.get().containsHeader("X-CSRF-Token")).isFalse();
            // verify
        }

        @Test
        @DisplayName("reconstructs only canonical refresh for refresh route")
        void reconstructsOnlyCanonicalRefreshForRefreshRoute() {
            // arrange
            MockServerHttpRequest request = MockServerHttpRequest.post("/auth/refresh")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer access")
                    .header("X-CSRF-Token", "raw-csrf")
                    .cookie(
                            new HttpCookie("__Host-refresh_token", "legacy"),
                            new HttpCookie("access_token", "access"),
                            new HttpCookie("csrf_token", "signed-csrf"))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<HttpHeaders> downstreamHeaders = new AtomicReference<>();
            GatewayFilterChain chain = current -> {
                downstreamHeaders.set(current.getRequest().getHeaders());
                return Mono.empty();
            };
            // conditions
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(downstreamHeaders.get().getFirst(HttpHeaders.COOKIE))
                    .isEqualTo("refresh_token=legacy");
            assertThat(downstreamHeaders.get().containsHeader(HttpHeaders.AUTHORIZATION)).isFalse();
            assertThat(downstreamHeaders.get().containsHeader("X-CSRF-Token")).isFalse();
            // verify
        }

        @Test
        @DisplayName("rejects refresh conflict and expires every migration identity")
        void rejectsRefreshConflictAndExpiresEveryMigrationIdentity() {
            // arrange
            MockServerHttpRequest request = MockServerHttpRequest.post("/auth/refresh")
                    .cookie(
                            new HttpCookie("refresh_token", "one"),
                            new HttpCookie("__Secure-refresh_token", "two"))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<Boolean> forwarded = new AtomicReference<>(false);
            GatewayFilterChain chain = current -> {
                forwarded.set(true);
                return Mono.empty();
            };
            // conditions
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(forwarded).hasValue(false);
            assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(400);
            List<ResponseCookie> deletionCookies = exchange.getResponse().getCookies().values().stream()
                    .flatMap(List::stream)
                    .toList();
            assertThat(deletionCookies)
                    .hasSize(5)
                    .allSatisfy(cookie -> assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO));
            // verify
        }

        @Test
        @DisplayName("preseves the cahced form body and hiden csrf field")
        void preservesTheCachedFormBodyAndHiddenCsrfField() {
            // arrange
            String body = "csrfmiddlewaretoken=raw-csrf&email=user%40example.test";
            MockServerHttpRequest request = MockServerHttpRequest.post("/auth/sign-in")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .cookie(new HttpCookie("csrf_token", "signed-csrf"))
                    .body(body);
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<String> downstreamBody = new AtomicReference<>();
            AtomicReference<String> downstreamCookie = new AtomicReference<>();
            GatewayFilterChain chain = current -> {
                downstreamCookie.set(current.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE));
                return DataBufferUtils.join(current.getRequest().getBody())
                        .doOnNext(buffer -> {
                            downstreamBody.set(buffer.toString(StandardCharsets.UTF_8));
                            DataBufferUtils.release(buffer);
                        })
                        .then();
            };
            // conditions
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(downstreamCookie).hasValue("csrf_token=signed-csrf");
            assertThat(downstreamBody).hasValue(body);
            // verify
        }

        @Test
        @DisplayName("keeps only handoff and csrf on completion")
        void keepsOnlyHandoffAndCsrfOnCompletion() {
            // arrange
            MockServerHttpRequest request = MockServerHttpRequest.get("/auth/admin/session")
                    .cookie(
                            new HttpCookie("access_token", "access"),
                            new HttpCookie("refresh_token", "refresh"),
                            new HttpCookie("admin_session_handoff", "handoff"),
                            new HttpCookie("csrf_token", "csrf"))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<String> downstreamCookie = new AtomicReference<>();
            GatewayFilterChain chain = current -> {
                downstreamCookie.set(current.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE));
                return Mono.empty();
            };
            // conditions
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(downstreamCookie).hasValue("admin_session_handoff=handoff; csrf_token=csrf");
            // assert
        }

    }

    @Nested
    @DisplayName("order()")
    class OrderTests {

        @Test
        @DisplayName("runs after trusted identity reconstruction and csrf")
        void runsAfterTrustedIdentityReconstructionAndCsrf() {
            // arrange + act + assert
            assertThat(GatewayFilterOrders.STRIP_INBOUND_AUTH_HEADERS)
                    .isLessThan(GatewayFilterOrders.CLIENT_METADATA_HEADERS);
            assertThat(GatewayFilterOrders.CLIENT_METADATA_HEADERS)
                    .isLessThan(GatewayFilterOrders.AUTHENTICATED_PASSPORT_HEADERS);
            assertThat(GatewayFilterOrders.AUTHENTICATED_PASSPORT_HEADERS)
                    .isLessThan(GatewayFilterOrders.CSRF);
            assertThat(GatewayFilterOrders.CSRF)
                    .isLessThan(filter.getOrder());
            // verify
        }

        @Test
        @DisplayName("authentication and csrf consume credentials before isolation")
        void authenticationAndCsrfConsumeCredentialsBeforeIsolation() {
            // arrange
            CsrfTokenService csrfTokenService = mock(CsrfTokenService.class);
            CsrfMiddlewareFilter csrfFilter = new CsrfMiddlewareFilter(cookieFactory, csrfTokenService);
            CookieOrBearerServerAuthenticationConverter converter = new CookieOrBearerServerAuthenticationConverter(
                    cookieFactory);
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/users/me")
                    .header("X-CSRF-Token", "raw-csrf")
                    .cookie(
                            new HttpCookie("access_token", "access-token"),
                            new HttpCookie("csrf_token", "signed-csrf"))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<HttpHeaders> downstreamHeaders = new AtomicReference<>();
            GatewayFilterChain route = current -> {
                downstreamHeaders.set(current.getRequest().getHeaders());
                return Mono.empty();
            };
            GatewayFilterChain isolation = current -> filter.filter(current, route);
            // conditions
            when(csrfTokenService.matches("signed-csrf", "raw-csrf")).thenReturn(true);
            // act
            Authentication authentication = converter.convert(exchange).block();
            csrfFilter.filter(exchange, isolation).block();
            // assert
            assertThat(authentication).isNotNull();
            assertThat(downstreamHeaders.get().containsHeader(HttpHeaders.COOKIE)).isFalse();
            assertThat(downstreamHeaders.get().containsHeader("X-CSRF-Token")).isFalse();
            // verify
        }

    }

}
