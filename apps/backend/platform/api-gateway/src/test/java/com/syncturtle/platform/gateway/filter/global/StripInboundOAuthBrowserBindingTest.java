package com.syncturtle.platform.gateway.filter.global;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.syncturtle.common.core.header.GatewayHeaders;

import reactor.core.publisher.Mono;

class StripInboundOAuthBrowserBindingTest {

    @Test
    void discardsClientSuppliedOAuthBindingBeforeGatewayCreatesTrustedHash() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/google/callback")
                .header(GatewayHeaders.HDR_OAUTH_BROWSER_BINDING, "spoofed").build());
        AtomicReference<String> forwarded = new AtomicReference<>();
        new StripInboundAuthHeadersFilter().filter(exchange, current -> {
            forwarded.set(current.getRequest().getHeaders().getFirst(GatewayHeaders.HDR_OAUTH_BROWSER_BINDING));
            return Mono.empty();
        }).block();
        assertThat(forwarded.get()).isNull();
    }
}
