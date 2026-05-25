package com.syncturtle.platform.gateway.filters.global;

import static com.syncturtle.common.core.cookie.CsrfConstants.CSRF_HEADER_NAME;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.csrf.CsrfTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsrfMiddlewareFilter implements GlobalFilter {

    private static final List<HttpMethod> SAFE = List.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    private final SecurityCookieFactory cookieFactory;
    private final CsrfTokenService csrfTokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isSafe(request) || isFormUrlEncoded(request.getHeaders())) {
            return chain.filter(exchange);
        }

        String signedToken = readCookie(request, cookieFactory.csrfCookieName());
        String submittedRawToken = request.getHeaders().getFirst(CSRF_HEADER_NAME);

        if (!csrfTokenService.matches(signedToken, submittedRawToken)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private static boolean isSafe(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        return method != null && SAFE.contains(method);
    }

    private static boolean isFormUrlEncoded(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        return contentType != null && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType);
    }

    private static String readCookie(ServerHttpRequest request, String cookieName) {
        HttpCookie cookie = request.getCookies().getFirst(cookieName);

        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return null;
        }

        return cookie.getValue().trim();
    }

}
