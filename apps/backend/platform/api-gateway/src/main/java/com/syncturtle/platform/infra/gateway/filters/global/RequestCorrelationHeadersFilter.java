package com.syncturtle.platform.infra.gateway.filters.global;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.constants.GatewayHeaderNames;

import reactor.core.publisher.Mono;

@Component
public class RequestCorrelationHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders in = request.getHeaders();

        // Do not trust inbound X-Auth-User-Id from clients
        String requestId = firstNonBlank(in.getFirst(GatewayHeaderNames.HDR_REQUEST_ID)).orElseGet(this::newId);
        String correlationId = firstNonBlank(in.getFirst(GatewayHeaderNames.HDR_CORRELATION_ID)).orElseGet(this::newId);

        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(h -> {
                    // remove any spoofed auth header
                    h.remove(GatewayHeaderNames.HDR_AUTH_USER_ID);

                    // ensure ids exists
                    h.set(GatewayHeaderNames.HDR_REQUEST_ID, requestId);
                    h.set(GatewayHeaderNames.HDR_CORRELATION_ID, correlationId);
                })
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        // Echo IDs back to caller on response
        mutatedExchange.getResponse().beforeCommit(() -> {
            HttpHeaders out = mutatedExchange.getResponse().getHeaders();
            out.set(GatewayHeaderNames.HDR_REQUEST_ID, requestId);
            out.set(GatewayHeaderNames.HDR_CORRELATION_ID, correlationId);

            out.remove(GatewayHeaderNames.HDR_AUTH_USER_ID);

            return Mono.empty();
        });

        return chain.filter(mutatedExchange);
    }

    private Optional<String> firstNonBlank(String value) {
        if (value == null)
            return Optional.empty();
        String trimmed = value.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }

}
