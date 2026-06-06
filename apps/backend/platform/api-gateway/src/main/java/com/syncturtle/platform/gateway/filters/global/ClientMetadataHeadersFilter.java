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

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 5;
    private static final int MAX_TRUSTED_INDEX = 2;

    private final RemoteAddressResolver remoteAddressResolver = XForwardedRemoteAddressResolver
            .maxTrustedIndex(MAX_TRUSTED_INDEX);

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = resolveClientIp(exchange);
        String userAgent = blankToNull(exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.USER_AGENT));

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.HDR_CLIENT_IP);
                    headers.remove(GatewayHeaders.HDR_CLIENT_UA);

                    if (clientIp != null) {
                        headers.set(GatewayHeaders.HDR_CLIENT_IP, clientIp);
                    }
                    if (userAgent != null) {
                        headers.set(GatewayHeaders.HDR_CLIENT_UA, userAgent);
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedIp = resolveForwardedIp(exchange);

        if (forwardedIp != null) {
            return forwardedIp;
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return null;
    }

    private String resolveForwardedIp(ServerWebExchange exchange) {
        InetSocketAddress address = remoteAddressResolver.resolve(exchange);
        if (address == null || address.getAddress() == null) {
            return null;
        }

        return blankToNull(address.getAddress().getHostAddress());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
