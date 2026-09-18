package com.syncturtle.platform.gateway.filter.global;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.syncturtle.common.core.header.GatewayHeaders;

import reactor.core.publisher.Mono;

class StripInboundAuthHeadersFilterTest {

    @Test
    void stripsFakeProvenanceActorIdentityAndClientMetadataBeforeAnyTrustReconstruction() {
        // arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me")
                        .header(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE, "fake")
                        .header(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID, "fake-actor")
                        .header(GatewayHeaders.HDR_AUTH_USER_ID, "fake-user")
                        .header(GatewayHeaders.HDR_CLIENT_IP, "198.51.100.7")
                        .header("X-Unrelated", "preserved")
                        .build());
        AtomicReference<HttpHeaders> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = current -> {
            forwarded.set(current.getRequest().getHeaders());
            return Mono.empty();
        };
        // conditions
        // act
        new StripInboundAuthHeadersFilter().filter(exchange, chain).block();
        // assert
        assertThat(forwarded.get().containsHeader(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE)).isFalse();
        assertThat(forwarded.get().containsHeader(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID)).isFalse();
        assertThat(forwarded.get().containsHeader(GatewayHeaders.HDR_AUTH_USER_ID)).isFalse();
        assertThat(forwarded.get().containsHeader(GatewayHeaders.HDR_CLIENT_IP)).isFalse();
        assertThat(forwarded.get().getFirst("X-Unrelated")).isEqualTo("preserved");
        // verify
    }

}
