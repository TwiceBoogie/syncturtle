package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;

class StripInboundPreAuthTransactionBindingTest {

    @Test
    @DisplayName("removes client supplied binding before trusted filters run")
    void removesClientSuppliedBindingBeforeTrustedFiltersRun() {
        // arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/path")
                .header(HDR_PREAUTH_TRANSACTION_BINDING, "spoofed").build());
        AtomicReference<String> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = current -> {
            forwarded.set(current.getRequest().getHeaders().getFirst(HDR_PREAUTH_TRANSACTION_BINDING));
            return Mono.empty();
        };
        // conditions
        // act
        new StripInboundAuthHeadersFilter().filter(exchange, chain).block();
        // assert
        assertThat(forwarded).hasValue(null);
    }

}
