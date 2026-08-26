package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.cookie.CsrfConstants.CSRF_FORM_FIELD_NAME;
import static com.syncturtle.common.core.cookie.CsrfConstants.CSRF_HEADER_NAME;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.cacheRequestBody;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.contracts.auth.session.PreAuthTransactionBinding;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.csrf.CsrfTokenService;
import com.syncturtle.platform.gateway.filter.GatewayFilterOrders;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public final class CsrfMiddlewareFilter implements GlobalFilter, Ordered {

    private static final List<HttpMethod> SAFE_METHODS = List.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
    private static final Set<String> ADMIN_HANDOFF_START_PATHS = Set.of(
            "/api/instances/admins/sign-in",
            "/api/instances/admins/sign-up");
    private static final FormHttpMessageReader FORM_READER = new FormHttpMessageReader();
    private static final ResolvableType FORM_DATA_TYPE = ResolvableType.forClassWithGenerics(
            MultiValueMap.class,
            String.class,
            String.class);

    private final SecurityCookieFactory cookieFactory;
    private final CsrfTokenService csrfTokenService;

    @Override
    public int getOrder() {
        return GatewayFilterOrders.CSRF;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isSafe(request)) {
            return chain.filter(exchange);
        }

        String signedToken = readCookie(request, cookieFactory.csrfCookieName());
        if (isFormUrlEncoded(request)) {
            return cacheRequestBody(exchange, cachedRequest -> {
                ServerWebExchange cachedExchange = exchange.mutate().request(cachedRequest).build();
                return readFormData(cachedRequest)
                        .flatMap(formData -> validateOrReject(
                                cachedExchange,
                                chain,
                                signedToken,
                                submittedFormToken(
                                        cachedRequest.getHeaders().getFirst(CSRF_HEADER_NAME),
                                        formData.getFirst(CSRF_FORM_FIELD_NAME))));
            });
        }

        String submittedToken = request.getHeaders().getFirst(CSRF_HEADER_NAME);
        return validateOrReject(exchange, chain, signedToken, submittedToken);
    }

    private Mono<Void> validateOrReject(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            String signedToken,
            String submittedToken) {
        if (csrfTokenService.matches(signedToken, submittedToken)) {
            if (!requiresPreAuthTransactionBinding(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            String binding = PreAuthTransactionBinding.fromValidatedSignedCsrfToken(signedToken).getValue();
            ServerHttpRequest trustedRequest = exchange.getRequest().mutate()
                    .headers(headers -> headers.set(HDR_PREAUTH_TRANSACTION_BINDING, binding))
                    .build();
            return chain.filter(exchange.mutate().request(trustedRequest).build());
        }

        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = """
                {"ok":false,"error":"INVALID_CSRF_TOKEN","message":"Invalid CSRF token."}
                """.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private static boolean isSafe(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        return method != null && SAFE_METHODS.contains(method);
    }

    private static boolean requiresPreAuthTransactionBinding(ServerHttpRequest request) {
        if (!HttpMethod.POST.equals(request.getMethod())) {
            return false;
        }

        String path = request.getPath().pathWithinApplication().value();
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return ADMIN_HANDOFF_START_PATHS.contains(path);
    }

    private static boolean isFormUrlEncoded(ServerHttpRequest request) {
        MediaType contentType = request.getHeaders().getContentType();
        return contentType != null && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType);
    }

    private static String submittedFormToken(String headerToken, String formToken) {
        if (headerToken == null || headerToken.isBlank()) {
            return formToken;
        }
        if (formToken == null || formToken.isBlank()) {
            return headerToken;
        }
        if (!headerToken.trim().equals(formToken.trim())) {
            return null;
        }
        return headerToken.trim();
    }

    private static Mono<MultiValueMap<String, String>> readFormData(ServerHttpRequest request) {
        return FORM_READER.readMono(FORM_DATA_TYPE, request, Map.of());
    }

    private static String readCookie(ServerHttpRequest request, String cookieName) {
        List<HttpCookie> cookies = request.getCookies().get(cookieName);
        if (cookies == null || cookies.size() != 1) {
            return null;
        }

        String value = cookies.getFirst().getValue();
        return value == null || value.isBlank() ? null : value.trim();
    }

}
