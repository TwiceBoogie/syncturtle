package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.syncturtle.common.contracts.auth.session.PreAuthTransactionBinding;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfRoutePolicy;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenProcessor;
import com.syncturtle.platform.gateway.security.csrf.ValidatedPreAuthCsrfToken;

import reactor.core.publisher.Mono;

class CsrfPreAuthTransactionBindingTest {

    @Nested
    class FilterTests {

        @Test
        @DisplayName("adds binding derived from the validated signed cookie only")
        void addsBindingDerivedFromTheValidatedSignedCookieOnly() {
            // arrange
            SecurityCookieFactory cookies = mock(SecurityCookieFactory.class);
            GatewayCsrfTokenProcessor tokenProcessor = mock(GatewayCsrfTokenProcessor.class);
            // conditions
            when(cookies.csrfCookieName()).thenReturn("csrf_token");
            when(tokenProcessor.validatePreAuth("signed-cookie", "submitted-secret")).thenReturn(
                    new ValidatedPreAuthCsrfToken(
                            "submitted-secret",
                            "signed-cookie",
                            Instant.parse("2026-08-27T12:00:00Z"),
                            Instant.parse("2026-08-27T12:30:00Z")));
            CsrfMiddlewareFilter filter = new CsrfMiddlewareFilter(cookies, tokenProcessor,
                    new GatewayCsrfRoutePolicy());
            MockServerWebExchange exchange = MockServerWebExchange
                    .from(MockServerHttpRequest.post("/api/instances/admins/sign-in")
                            .cookie(new HttpCookie("csrf_token", "signed-cookie"))
                            .header("X-CSRF-Token", "submitted-secret")
                            .header(HDR_PREAUTH_TRANSACTION_BINDING, "attacker-value").build());
            AtomicReference<String> forwarded = new AtomicReference<>();
            GatewayFilterChain chain = current -> {
                forwarded.set(current.getRequest().getHeaders().getFirst(HDR_PREAUTH_TRANSACTION_BINDING));
                return Mono.empty();
            };
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(forwarded)
                    .hasValue(PreAuthTransactionBinding.fromValidatedSignedCsrfToken("signed-cookie").getValue());
        }

    }

}
