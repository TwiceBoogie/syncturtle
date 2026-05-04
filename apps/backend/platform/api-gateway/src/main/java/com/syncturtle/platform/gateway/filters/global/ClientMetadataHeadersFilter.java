package com.syncturtle.platform.gateway.filters.global;

import java.net.InetSocketAddress;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.header.GatewayHeaders;

import reactor.core.publisher.Mono;

@Component
public class ClientMetadataHeadersFilter implements GlobalFilter, Ordered {

    private final RemoteAddressResolver remoteAddressResolver = XForwardedRemoteAddressResolver.maxTrustedIndex(2);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String clientIp = resolveClientIp(exchange);
        String ua = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);

        ServerHttpRequest mutated = request.mutate()
                .headers(h -> {
                    h.remove(GatewayHeaders.HDR_CLIENT_IP);
                    h.remove(GatewayHeaders.HDR_CLIENT_UA);

                    if (clientIp != null && !clientIp.isBlank()) {
                        h.set(GatewayHeaders.HDR_CLIENT_IP, clientIp);
                    }
                    if (ua != null && !ua.isBlank()) {
                        h.set(GatewayHeaders.HDR_CLIENT_UA, ua);
                    }
                }).build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String cloudFlare = headers.getFirst("CF-Connecting-IP");
        if (cloudFlare != null && !cloudFlare.isBlank()) {
            return cloudFlare.trim();
        }

        InetSocketAddress addr = remoteAddressResolver.resolve(exchange);
        if (addr != null && addr.getAddress() != null) {
            return addr.getAddress().getHostAddress();
        }

        // fallback
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }

        return "unknown";
    }

}
