package com.syncturtle.platform.gateway.filters.global;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.spring.properties.CsrfTransportProperties;
import com.syncturtle.common.web.csrf.CsrfTokenSigner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsrfMiddlewareFilter implements GlobalFilter {

    private static final List<HttpMethod> SAFE = List.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    private final CsrfTransportProperties props;
    private final CsrfTokenSigner csrfTokenSigner;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 1: Only enfore on unsafe methods
        if (isMethodSafe(request)) {
            return chain.filter(exchange);
        }

        // 2: Only for JSON, x-www-form-urlencoded continue as downstream services deals
        if (isFormUrlEncoded(request.getHeaders())) {
            return chain.filter(exchange);
        }
        // 2: grab csrf cookie + custom header
        String signedCookie = readCookie(request.getCookies().get(props.getCookieName()));
        String rawHeader = request.getHeaders().getFirst(props.getHeaderName());
        log.info("CSRF Cookie: {}, CSRF Header: {}", signedCookie, rawHeader);
        // 3: validate csrf
        if (signedCookie == null || rawHeader == null || rawHeader.isBlank()) {
            return forbidden(exchange.getResponse());
        }

        if (!csrfTokenSigner.verify(signedCookie)) {
            return forbidden(exchange.getResponse());
        }

        String rawCookieToken = csrfTokenSigner.extractToken(signedCookie);
        if (rawCookieToken == null || !constantTimeEquals(rawCookieToken, rawHeader.trim())) {
            return forbidden(exchange.getResponse());
        }
        return chain.filter(exchange);
    }

    private static boolean isMethodSafe(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        return method != null && SAFE.contains(method);
    }

    private static boolean isFormUrlEncoded(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        return contentType != null && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType);
    }

    private static String readCookie(List<HttpCookie> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        String cookie = cookies.getFirst().getValue();
        return (cookie == null || cookie.isBlank()) ? null : cookie.trim();
    }

    private static Mono<Void> forbidden(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);

        int diff = x.length ^ y.length;
        for (int i = 0; i < Math.min(x.length, y.length); i++) {
            diff |= x[i] ^ y[i];
        }
        return diff == 0;
    }

}
