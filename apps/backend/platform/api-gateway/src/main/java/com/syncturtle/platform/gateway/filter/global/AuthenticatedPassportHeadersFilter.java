package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_ADMIN_SESSION_VERSION;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_INSTANCE_ID;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_ISSUER;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_ROLES;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_SESSION_ID;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_USER_AUTH_VERSION;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_USER_ID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.platform.gateway.filter.GatewayFilterOrders;
import com.syncturtle.platform.gateway.security.PassportAuthenticationToken;
import com.syncturtle.platform.gateway.security.session.ParsedPassportSession;

import reactor.core.publisher.Mono;

@Component
public final class AuthenticatedPassportHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return GatewayFilterOrders.AUTHENTICATED_PASSPORT_HEADERS;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .ofType(PassportAuthenticationToken.class)
                .flatMap(authentication -> forwardAuthenticated(exchange, chain, authentication)
                        .thenReturn(true))
                .defaultIfEmpty(false)
                .flatMap(forwarded -> forwarded
                        ? Mono.empty()
                        : chain.filter(exchange));
    }

    private Mono<Void> forwardAuthenticated(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            PassportAuthenticationToken authentication) {
        ParsedPassportSession session = authentication.getValidatedSession();
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> replaceTrustedHeaders(headers, authentication, session))
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private void replaceTrustedHeaders(
            HttpHeaders headers,
            PassportAuthenticationToken authentication,
            ParsedPassportSession session) {
        removeTrustedHeaders(headers);
        headers.set(HDR_AUTH_USER_ID, session.getUserId());
        headers.set(HDR_AUTH_SESSION_ID, session.getSessionId());
        headers.set(HDR_AUTH_INSTANCE_ID, session.getInstanceId());
        headers.set(HDR_AUTH_ROLES, String.join(",", session.getRoles()));
        headers.set(HDR_AUTH_USER_AUTH_VERSION, Long.toString(session.getUserAuthVersion()));
        headers.set(HDR_AUTH_ISSUER, authentication.getToken().getIssuer().toString());

        if (session.getAdminSessionVersion() != null) {
            headers.set(HDR_AUTH_ADMIN_SESSION_VERSION, session.getAdminSessionVersion().toString());
        }
    }

    private void removeTrustedHeaders(HttpHeaders headers) {
        headers.remove(HDR_AUTH_USER_ID);
        headers.remove(HDR_AUTH_SESSION_ID);
        headers.remove(HDR_AUTH_INSTANCE_ID);
        headers.remove(HDR_AUTH_ROLES);
        headers.remove(HDR_AUTH_USER_AUTH_VERSION);
        headers.remove(HDR_AUTH_ADMIN_SESSION_VERSION);
        headers.remove(HDR_AUTH_ISSUER);
    }

}
