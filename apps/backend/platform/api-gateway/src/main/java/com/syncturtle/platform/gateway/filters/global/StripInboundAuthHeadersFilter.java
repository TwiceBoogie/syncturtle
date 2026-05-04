package com.syncturtle.platform.gateway.filters.global;

import java.util.Set;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Security filter designed to prevent <b>Header Spoofing</b> attacks.
 * 
 * <p>
 * Downstream microservices rely on the {@code X-Auth-User-Id} header to
 * populate
 * security contexts (e.g., {@code ThreadLocal} storage). If a client provides
 * this
 * header in an inbound request, they could potentially impersonate any user.
 * 
 * <p>
 * This filter scrubs these sensitive headers from the request immediately upon
 * entry. The Gateway will later re-inject a <b>trusted</b> value after
 * successful
 * authentication is performed.
 * 
 * @author Salvador Sebastian
 * @see #filter(ServerWebExchange, GatewayFilterChain)
 */
@Component
public class StripInboundAuthHeadersFilter implements GlobalFilter, Ordered {

    /**
     * Target header used by downstream services for user identification.
     */
    private static final Set<String> BLOCKED = Set.of(
            "X-Auth-User-Id",
            "X-Auth-Session-Id",
            "X-Auth-Token-Version",
            "X-Auth-Instance-Id",
            "X-Auth-Roles",
            "X-Auth-Issuer");

    /**
     * Defines the execution priority.
     * 
     * <p>
     * Returns {@link Ordered#HIGHEST_PRECEDENCE} to ensure sanitization
     * occurs before any other logic or routing takes place.
     * 
     * @return {@code Integer.MIN_VALUE}
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Sanitizes the inbound request by stripping unverified identity headers.
     * 
     * <p>
     * This operation ensures that any {@code X-Auth-User-Id} present in the
     * {@link ServerWebExchange} is removed before the request moves further
     * into the internal network.
     * 
     * <p>
     * <b>Warning:</b> Failure to execute this filter early in the chain
     * may allow unauthorized identity propagation to downstream {@code ThreadLocal}
     * states.
     * 
     * @param exchange the current reactive server exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} indicating the completion of the filtering logic
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> BLOCKED.forEach(headers::remove))
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

}
