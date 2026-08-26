package com.syncturtle.platform.gateway.filter.global;

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
import com.syncturtle.platform.gateway.configuration.property.GatewayClientMetadataProperties;
import com.syncturtle.platform.gateway.filter.GatewayFilterOrders;

import reactor.core.publisher.Mono;

@Component
public class ClientMetadataHeadersFilter implements GlobalFilter, Ordered {

    private final RemoteAddressResolver remoteAddressResolver;

    public ClientMetadataHeadersFilter(GatewayClientMetadataProperties properties) {
        int trustedProxyCount = properties.getTrustedProxyCount();

        this.remoteAddressResolver = trustedProxyCount == 0
                ? null
                : XForwardedRemoteAddressResolver.maxTrustedIndex(trustedProxyCount);
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.CLIENT_METADATA_HEADERS;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = resolveClientIp(exchange);
        String userAgent = blankToNull(
                exchange.getRequest()
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
        if (remoteAddressResolver != null) {
            InetSocketAddress forwardedAddress = remoteAddressResolver.resolve(exchange);

            String forwardedIp = addressValue(forwardedAddress);

            if (forwardedIp != null) {
                return forwardedIp;
            }
        }

        return addressValue(exchange.getRequest().getRemoteAddress());
    }

    private static String addressValue(InetSocketAddress address) {
        if (address == null || address.getAddress() == null) {
            return null;
        }

        return blankToNull(address.getAddress().getHostAddress());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
