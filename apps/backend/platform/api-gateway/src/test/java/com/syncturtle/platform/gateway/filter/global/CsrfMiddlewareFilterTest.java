package com.syncturtle.platform.gateway.filter.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.csrf.CsrfTokenService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@DisplayName("CsrfMiddlewareFilter")
class CsrfMiddlewareFilterTest {

    private static final String COOKIE_NAME = "csrf_token";
    private static final String SIGNED_TOKEN = "signed-token";
    private static final String RAW_TOKEN = "raw-token";

    private CsrfTokenService tokenService;
    private CsrfMiddlewareFilter filter;

    @BeforeEach
    void setup() {
        SecurityCookieFactory cookieFactory = mock(SecurityCookieFactory.class);
        when(cookieFactory.csrfCookieName()).thenReturn(COOKIE_NAME);
        tokenService = mock(CsrfTokenService.class);
        filter = new CsrfMiddlewareFilter(cookieFactory, tokenService);
    }

    @Nested
    @DisplayName("filter(ServerWebExchange, GatewayFilterChain)")
    class FilterTests {

        @Test
        @DisplayName("permits safe requests without csrf pair")
        void permitsSafeRequestWithoutCsrfPair() {
            // arrange
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/users/me").build());
            AtomicBoolean forwarded = new AtomicBoolean();
            GatewayFilterChain chain = current -> {
                forwarded.set(true);
                return Mono.empty();
            };
            // conditions
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(forwarded).isTrue();
            // verify
            verifyNoInteractions(tokenService);
        }

        @Test
        @DisplayName("permits json refresh with matching header and cookie")
        void permitsJsonRefreshWithMatchingHeaderAndCookie() {
            // arrange
            MockServerHttpRequest request = MockServerHttpRequest.post("/auth/refresh")
                    .cookie(new HttpCookie(COOKIE_NAME, SIGNED_TOKEN))
                    .header("X-CSRF-Token", RAW_TOKEN)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicBoolean forwarded = new AtomicBoolean();
            GatewayFilterChain chain = current -> {
                forwarded.set(true);
                return Mono.empty();
            };
            // conditions
            when(tokenService.matches(SIGNED_TOKEN, RAW_TOKEN)).thenReturn(true);
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(forwarded).isTrue();
            // verify
        }

        @Test
        @DisplayName("permits the final admin logout form and preserves its body")
        void permitsTheFinalAdminLogoutFormAndPreservesItsBody() {
            // arrange
            String body = "csrfmiddlewaretoken=" + RAW_TOKEN;
            MockServerHttpRequest request = MockServerHttpRequest.post("/auth/admin/sign-out")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .cookie(new HttpCookie(COOKIE_NAME, SIGNED_TOKEN))
                    .body(body);
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<String> forwardedBody = new AtomicReference<>();
            GatewayFilterChain chain = current -> DataBufferUtils.join(current.getRequest().getBody())
                    .doOnNext(buffer -> {
                        forwardedBody.set(buffer.toString(StandardCharsets.UTF_8));
                        DataBufferUtils.release(buffer);
                    })
                    .then();
            // conditions
            when(tokenService.matches(SIGNED_TOKEN, RAW_TOKEN)).thenReturn(true);
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(forwardedBody).hasValue(body);
        }

        @Test
        @DisplayName("rejects form when header and field conflict")
        void rejectsFormWhenHeaderAndFieldConflict() {
            // arrange
            MockServerHttpRequest request = MockServerHttpRequest.post("/auth/sign-in")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .cookie(new HttpCookie(COOKIE_NAME, SIGNED_TOKEN))
                    .header("X-CSRF-Token", "different-token")
                    .body("csrfmiddlewaretoken=" + RAW_TOKEN);
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicBoolean forwarded = new AtomicBoolean();
            GatewayFilterChain chain = current -> current.getFormData()
                    .doOnNext(formData -> forwarded.set(
                            RAW_TOKEN.equals(formData.getFirst("csrfmiddlewaretoken"))))
                    .then();
            // conditions
            when(tokenService.matches(SIGNED_TOKEN, null)).thenReturn(false);
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(forwarded).isFalse();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            // verify
        }

        @Test
        @DisplayName("preservices a one shot form body for downstream isolation and routing")
        void preservesAOneShotFormBodyForDownstreamIsolationAndRouting() {
            // arrange
            String requestBody = "csrfmiddlewaretoken=" + RAW_TOKEN + "&firstName=Luna";
            AtomicInteger bodySubscriptions = new AtomicInteger();
            Flux<DataBuffer> oneShotBody = Flux.defer(() -> {
                if (bodySubscriptions.getAndIncrement() > 0) {
                    return Flux.empty();
                }
                return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(
                        requestBody.getBytes(StandardCharsets.UTF_8)));
            });
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/instances/admins/sign-up")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .cookie(new HttpCookie(COOKIE_NAME, SIGNED_TOKEN))
                    .body(oneShotBody);
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<String> forwardedBody = new AtomicReference<>();
            GatewayFilterChain chain = current -> DataBufferUtils.join(current.getRequest().getBody())
                    .doOnNext(buffer -> {
                        forwardedBody.set(buffer.toString(StandardCharsets.UTF_8));
                        DataBufferUtils.release(buffer);
                    })
                    .then();
            // conditions
            when(tokenService.matches(SIGNED_TOKEN, RAW_TOKEN)).thenReturn(true);
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(bodySubscriptions).hasValue(1);
            assertThat(forwardedBody).hasValue(requestBody);
        }

        @Test
        void permitsFormLogoutWithMatchingFormFieldAndCookie() {
            when(tokenService.matches(SIGNED_TOKEN, RAW_TOKEN)).thenReturn(true);
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/instances/admins/sign-out")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .cookie(new HttpCookie(COOKIE_NAME, SIGNED_TOKEN))
                    .body("csrfmiddlewaretoken=" + RAW_TOKEN);
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicBoolean forwarded = new AtomicBoolean();
            GatewayFilterChain chain = current -> current.getFormData()
                    .doOnNext(formData -> forwarded.set(
                            RAW_TOKEN.equals(formData.getFirst("csrfmiddlewaretoken"))))
                    .then();

            filter.filter(exchange, chain).block();

            assertThat(forwarded).isTrue();
        }

        @Test
        void permitsFormWithOneShotBodyAndPreservesBodyForForwarding() {
            // arrange
            String requestBody = "csrfmiddlewaretoken=" + RAW_TOKEN + "&firstName=Luna";
            AtomicInteger bodySubscriptions = new AtomicInteger();
            Flux<DataBuffer> oneShotBody = Flux.defer(() -> {
                if (bodySubscriptions.getAndIncrement() > 0) {
                    return Flux.empty();
                }
                byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
                return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bodyBytes));
            });
            when(tokenService.matches(SIGNED_TOKEN, RAW_TOKEN)).thenReturn(true);
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/instances/admins/sign-up")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .cookie(new HttpCookie(COOKIE_NAME, SIGNED_TOKEN))
                    .body(oneShotBody);
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<String> forwardedBody = new AtomicReference<>();
            GatewayFilterChain chain = current -> DataBufferUtils.join(current.getRequest().getBody())
                    .doOnNext(buffer -> {
                        forwardedBody.set(buffer.toString(StandardCharsets.UTF_8));
                        DataBufferUtils.release(buffer);
                    })
                    .then();

            // act
            filter.filter(exchange, chain).block();

            // assert
            assertThat(bodySubscriptions).hasValue(1);
            assertThat(forwardedBody).hasValue(requestBody);
        }

        @Test
        void rejectsUnsafeRequestWithoutMatchingPair() {
            when(tokenService.matches(any(), any())).thenReturn(false);
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/auth/refresh").build());
            AtomicBoolean forwarded = new AtomicBoolean();
            GatewayFilterChain chain = current -> {
                forwarded.set(true);
                return Mono.empty();
            };

            filter.filter(exchange, chain).block();

            assertThat(forwarded).isFalse();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

    }

}
