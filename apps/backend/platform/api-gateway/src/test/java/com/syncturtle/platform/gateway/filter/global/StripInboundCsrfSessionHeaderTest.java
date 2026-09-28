package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_INTERNAL_CSRF_SESSION_ID;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;

class StripInboundCsrfSessionHeaderTest {

    @Test
    void removesBrowserSuppliedTrustedCsrfSessionHeader() {
        // arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/refresh")
                        .header(HDR_INTERNAL_CSRF_SESSION_ID, "attacker-value")
                        .build());
        AtomicReference<String> forwarded = new AtomicReference<>();
        StripInboundAuthHeadersFilter filter = new StripInboundAuthHeadersFilter();
        // conditions
        // act
        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest().getHeaders().getFirst(HDR_INTERNAL_CSRF_SESSION_ID));
            return Mono.empty();
        }).block();
        // assert
        assertThat(forwarded.get()).isNull();
        // verify
    }

}
