package com.syncturtle.platform.gateway.filter.global;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.exception.RequestProvenanceException;
import com.syncturtle.common.security.provenance.RequestHeaderSource;
import com.syncturtle.common.security.provenance.RequestProvenanceSigner;
import com.syncturtle.platform.gateway.filter.GatewayFilterOrders;
import com.syncturtle.platform.gateway.security.provenance.GatewayRequestProvenancePolicy;

import reactor.core.publisher.Mono;

@Component
public final class RequestProvenanceGlobalFilter implements GlobalFilter, Ordered {

    private static final byte[] UNAVAILABLE_RESPONSE = "{\"code\":\"REQUEST_PROVENANCE_UNAVAILABLE\"}"
            .getBytes(StandardCharsets.UTF_8);

    private final RequestProvenanceSigner signer;
    private final GatewayRequestProvenancePolicy policy;

    public RequestProvenanceGlobalFilter(RequestProvenanceSigner signer, GatewayRequestProvenancePolicy policy) {
        Assert.notNull(signer, "signer is required");
        Assert.notNull(policy, "policy is required");

        this.signer = signer;
        this.policy = policy;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.REQUEST_PROVENANCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            if (route == null) {
                return unavailable(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();
            RequestHeaderSource headers = name -> {
                List<String> values = request.getHeaders().get(name);
                return values == null ? List.of() : values;
            };

            String assertion = signer.sign(
                    policy.requireAudience(route.getId()),
                    request.getMethod().name(),
                    request.getURI().getRawPath(),
                    request.getURI().getRawQuery(),
                    headers);

            ServerHttpRequest signedRequest = request.mutate()
                    .headers(httpHeaders -> {
                        httpHeaders.remove(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE);
                        httpHeaders.add(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE, assertion);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(signedRequest).build());
        } catch (RequestProvenanceException | IllegalStateException exception) {
            return unavailable(exchange);
        }
    }

    private static Mono<Void> unavailable(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(UNAVAILABLE_RESPONSE.length);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(UNAVAILABLE_RESPONSE);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

}
