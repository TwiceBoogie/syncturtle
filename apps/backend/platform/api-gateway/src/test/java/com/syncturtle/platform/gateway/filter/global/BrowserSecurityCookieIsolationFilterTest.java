package com.syncturtle.platform.gateway.filter.global;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.property.SecurityCookieProperties;
import com.syncturtle.platform.gateway.security.BrowserSecurityCookiePolicy;

import reactor.core.publisher.Mono;

class BrowserSecurityCookieIsolationFilterTest {

    private BrowserSecurityCookieIsolationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityCookieFactory cookies = new SecurityCookieFactory(
                new SecurityCookieProperties(false, false, "Lax", "Lax", "Lax"));
        filter = new BrowserSecurityCookieIsolationFilter(new BrowserSecurityCookiePolicy(cookies));
    }

    @Test
    void stripsAccessCookieAuthorizationAndBrowserPolicyHeaderFromDownstreamRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer forged")
                .header(GatewayHeaders.HDR_BROWSER_REQUEST, "1")
                .cookie(new HttpCookie("access_token", "access"), new HttpCookie("theme", "dark"))
                .build());
        AtomicReference<HttpHeaders> forwarded = new AtomicReference<>();
        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest().getHeaders());
            return Mono.empty();
        }).block();
        assertThat(forwarded.get().containsHeader(HttpHeaders.COOKIE)).isFalse();
        assertThat(forwarded.get().containsHeader(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(forwarded.get().containsHeader(GatewayHeaders.HDR_BROWSER_REQUEST)).isFalse();
    }

    @Test
    void refreshReceivesOnlyOneCanonicalOpaqueCookie() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/refresh")
                .cookie(new HttpCookie("__Secure-refresh_token", "opaque"),
                        new HttpCookie("access_token", "access"))
                .build());
        AtomicReference<String> forwarded = new AtomicReference<>();
        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE));
            return Mono.empty();
        }).block();
        assertThat(forwarded.get()).isEqualTo("refresh_token=opaque");
    }

    @Test
    void conflictingRefreshCookiesAreRejectedWithoutClearingAWinnersCookies() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/refresh")
                .cookie(new HttpCookie("refresh_token", "old"),
                        new HttpCookie("__Secure-refresh_token", "new"))
                .build());
        filter.filter(exchange, unused -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getCookies()).isEmpty();
    }

    @Test
    void preservesNativeFormBodyWhileRemovingBrowserCookies() {
        String body = "email=user%40example.test&password=example";
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .cookie(new HttpCookie("access_token", "old-access"))
                .body(body));
        AtomicReference<String> forwardedBody = new AtomicReference<>();
        AtomicReference<String> forwardedCookie = new AtomicReference<>();
        filter.filter(exchange, current -> {
            forwardedCookie.set(current.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE));
            return DataBufferUtils.join(current.getRequest().getBody())
                    .doOnNext(buffer -> {
                        forwardedBody.set(buffer.toString(StandardCharsets.UTF_8));
                        DataBufferUtils.release(buffer);
                    })
                    .then();
        }).block();
        assertThat(forwardedCookie.get()).isNull();
        assertThat(forwardedBody.get()).isEqualTo(body);
    }

    @Test
    void completionForwardsOnlyTheOneUseHandoffCookie() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/admin/session")
                .cookie(new HttpCookie("admin_session_handoff", "handoff"),
                        new HttpCookie("access_token", "old-access"),
                        new HttpCookie("refresh_token", "old-refresh"))
                .build());
        AtomicReference<String> forwarded = new AtomicReference<>();
        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE));
            return Mono.empty();
        }).block();
        assertThat(forwarded.get()).isEqualTo("admin_session_handoff=handoff");
    }

    @Test
    void keepsTrustedPassportHeadersWhileDiscardingBrowserCredentials() {
        String userId = "11111111-1111-1111-1111-111111111111";
        String sessionId = "22222222-2222-2222-2222-222222222222";
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.patch("/api/users/me")
                .header(GatewayHeaders.HDR_AUTH_USER_ID, userId)
                .header(GatewayHeaders.HDR_AUTH_SESSION_ID, sessionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer access")
                .cookie(new HttpCookie("access_token", "access"))
                .build());
        AtomicReference<HttpHeaders> forwarded = new AtomicReference<>();
        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest().getHeaders());
            return Mono.empty();
        }).block();
        assertThat(forwarded.get().getFirst(GatewayHeaders.HDR_AUTH_USER_ID)).isEqualTo(userId);
        assertThat(forwarded.get().getFirst(GatewayHeaders.HDR_AUTH_SESSION_ID)).isEqualTo(sessionId);
        assertThat(forwarded.get().containsHeader(HttpHeaders.COOKIE)).isFalse();
        assertThat(forwarded.get().containsHeader(HttpHeaders.AUTHORIZATION)).isFalse();
    }
}
