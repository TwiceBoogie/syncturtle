package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_USER_ID;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.syncturtle.common.security.exception.RequestProvenanceException;
import com.syncturtle.common.security.provenance.RequestProvenanceSigner;
import com.syncturtle.platform.gateway.filter.GatewayFilterOrders;
import com.syncturtle.platform.gateway.security.provenance.GatewayRequestProvenancePolicy;

import reactor.core.publisher.Mono;

class RequestProvenanceGlobalFilterTest {

    private RequestProvenanceSigner signer;
    private RequestProvenanceGlobalFilter filter;

    @BeforeEach
    void setup() {
        signer = mock(RequestProvenanceSigner.class);
        filter = new RequestProvenanceGlobalFilter(signer, new GatewayRequestProvenancePolicy());
    }

    @Nested
    class FilteringTests {

        @Test
        void signsFinalMethodRawPathQueryAndProtectedHeadersBeforeRouting() {
            // arrange
            MockServerWebExchange exchange = exchange("user-protected");
            AtomicReference<String> forwarded = new AtomicReference<>();
            GatewayFilterChain chain = current -> {
                forwarded.set(current.getRequest().getHeaders().getFirst(HDR_INTERNAL_REQUEST_PROVENANCE));
                return Mono.empty();
            };
            // conditions
            when(signer.sign(eq("user-service"), eq("GET"), eq("/api/users/me"), eq("view=full"), any()))
                    .thenReturn("fresh-assertion");
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(forwarded).hasValue("fresh-assertion");
            assertThat(filter.getOrder()).isEqualTo(GatewayFilterOrders.REQUEST_PROVENANCE).isEqualTo(9999);
            // verify
            verify(signer).sign(eq("user-service"), eq("GET"), eq("/api/users/me"), eq("view=full"), any());
        }

        @Test
        void returns503AndNeverForwardsWhenSigningFails() {
            // arrange
            MockServerWebExchange exchange = exchange("user-protected");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);
            // conditions
            when(signer.sign(any(), any(), any(), any(), any()))
                    .thenThrow(new RequestProvenanceException("signing failed"));
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            // verify
            verify(chain, never()).filter(any());
        }

        @Test
        void rejectsRouteWithoutACodeOwnedAudienceAndContainsAllCurrentMappings() {
            // arrange
            GatewayRequestProvenancePolicy policy = new GatewayRequestProvenancePolicy();
            MockServerWebExchange exchange = exchange("configuration-defined-route");
            GatewayFilterChain chain = mock(GatewayFilterChain.class);
            // conditions
            // act
            filter.filter(exchange, chain).block();
            // assert
            assertThat(policy.routeCount()).isEqualTo(20);
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            // verify
            verify(chain, never()).filter(any());
            verify(signer, never()).sign(any(), any(), any(), any(), any());
        }

    }

    private static MockServerWebExchange exchange(String routeId) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me?view=full")
                .header(HDR_AUTH_USER_ID, "user-1")
                .header(HDR_INTERNAL_REQUEST_PROVENANCE, "browser-forgery")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        Route route = Route.async()
                .id(routeId)
                .uri(URI.create("http://user-service:8080"))
                .predicate(candidate -> true)
                .build();
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        return exchange;
    }

}
