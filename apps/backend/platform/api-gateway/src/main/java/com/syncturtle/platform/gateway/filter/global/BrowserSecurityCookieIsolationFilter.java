package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.cookie.CsrfConstants.CSRF_HEADER_NAME;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.platform.gateway.filter.GatewayFilterOrders;
import com.syncturtle.platform.gateway.security.BrowserSecurityCookiePolicy;
import com.syncturtle.platform.gateway.security.BrowserSecurityCookiePolicy.Decision;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public final class BrowserSecurityCookieIsolationFilter implements GlobalFilter, Ordered {

    private final BrowserSecurityCookiePolicy policy;
    private final SecurityCookieFactory cookieFactory;

    @Override
    public int getOrder() {
        return GatewayFilterOrders.BROWSER_SECURITY_COOKIE_ISOLATION;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Decision decision = policy.decide(exchange.getRequest());
        if (decision.isRefreshConflict()) {
            return rejectRefreshConflict(exchange);
        }

        String downstreamCookieHeader = decision.getDownstreamCookies().stream()
                .map(HttpCookie::toString)
                .collect(Collectors.joining("; "));
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(HttpHeaders.COOKIE);
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.remove(CSRF_HEADER_NAME);

                    if (!downstreamCookieHeader.isEmpty()) {
                        headers.set(HttpHeaders.COOKIE, downstreamCookieHeader);
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private Mono<Void> rejectRefreshConflict(ServerWebExchange exchange) {
        cookieFactory.clearRefreshTokenCookies().forEach(exchange.getResponse()::addCookie);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = """
                {"ok":false,"error":"REFRESH_COOKIE_CONFLICT","message":"Conflicting refresh credentials."}
                """.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

}
