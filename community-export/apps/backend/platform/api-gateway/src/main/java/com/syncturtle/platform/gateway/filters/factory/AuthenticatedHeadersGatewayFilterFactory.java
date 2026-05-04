package com.syncturtle.platform.gateway.filters.factory;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AuthenticatedHeadersGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthenticatedHeadersGatewayFilterFactory.Config> {

    public AuthenticatedHeadersGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    String userId = auth.getToken().getSubject();
                    String sessionId = auth.getToken().getClaimAsString("sid");
                    String instanceId = auth.getToken().getClaimAsString("instance_id");
                    List<String> roles = auth.getToken().getClaimAsStringList("roles");

                    Long authVersion = claimAsLong(auth, "auth_ver");
                    Long adminSessionVersion = claimAsLong(auth, "admin_session_ver");

                    ServerHttpRequest request = exchange.getRequest()
                            .mutate()
                            .headers(headers -> {
                                headers.set("X-Auth-User-Id", userId);
                                headers.set("X-Auth-Session-Id", sessionId);
                                headers.set("X-Auth-Instance-Id", instanceId);
                                headers.set("X-Auth-Roles", roles == null ? "" : String.join(",", roles));

                                headers.set("X-Auth-User-Auth-Version",
                                        authVersion == null ? "" : String.valueOf(authVersion));

                                headers.set("X-Auth-Admin-Session-Version",
                                        adminSessionVersion == null ? "" : String.valueOf(adminSessionVersion));

                                headers.set("X-Auth-Issuer",
                                        auth.getToken().getIssuer() != null
                                                ? auth.getToken().getIssuer().toString()
                                                : "");
                            })
                            .build();

                    return chain.filter(exchange.mutate().request(request).build()).thenReturn(Boolean.TRUE);
                })
                .switchIfEmpty(Mono.defer(() -> unauthorized(exchange).thenReturn(Boolean.TRUE)))
                .then();
    }

    private Long claimAsLong(JwtAuthenticationToken auth, String claimName) {
        Object raw = auth.getToken().getClaim(claimName);

        if (raw instanceof Number number) {
            return number.longValue();
        }

        if (raw instanceof String string && !string.isBlank()) {
            return Long.parseLong(string);
        }

        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        byte[] body = """
                {"ok":false,"error":"UNAUTHORIZED","message":"Authentication required."}
                """.getBytes(StandardCharsets.UTF_8);

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(body)));
    }

    public static class Config {
    }
}