package com.syncturtle.platform.gateway.filters.global;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.header.GatewayHeaders;

import reactor.core.publisher.Mono;

@Component
public final class RequestCorrelationHeadersFilter implements GlobalFilter, Ordered {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
    private static final int MAX_ID_LENGTH = 128;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]+");

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        // Do not trust inbound X-Auth-User-Id from clients
        String requestId = sanitizeOrNew(headers.getFirst(GatewayHeaders.HDR_REQUEST_ID));
        String correlationId = sanitizeOrNew(headers.getFirst(GatewayHeaders.HDR_CORRELATION_ID));

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(mutatedHeaders -> {
                    mutatedHeaders.set(GatewayHeaders.HDR_REQUEST_ID, requestId);
                    mutatedHeaders.set(GatewayHeaders.HDR_CORRELATION_ID, correlationId);
                })
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();

        // Echo IDs back to caller on response
        mutatedExchange.getResponse().beforeCommit(() -> {
            HttpHeaders responseHeaders = mutatedExchange.getResponse().getHeaders();
            responseHeaders.set(GatewayHeaders.HDR_REQUEST_ID, requestId);
            responseHeaders.set(GatewayHeaders.HDR_CORRELATION_ID, correlationId);
            return Mono.empty();
        });

        return chain.filter(mutatedExchange);
    }

    private static String sanitizeOrNew(String value) {
        if (value == null || value.isBlank()) {
            return newId();
        }

        String trimmed = value.trim();
        if (trimmed.length() > MAX_ID_LENGTH || !SAFE_ID.matcher(trimmed).matches()) {
            return newId();
        }

        return trimmed;
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

}
