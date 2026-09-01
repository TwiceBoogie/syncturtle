package com.syncturtle.platform.gateway.filter.global;

import static com.syncturtle.common.core.cookie.CsrfConstants.CSRF_FORM_FIELD_NAME;
import static com.syncturtle.common.core.cookie.CsrfConstants.CSRF_HEADER_NAME;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_INTERNAL_CSRF_SESSION_ID;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.cacheRequestBody;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.contracts.auth.session.PreAuthTransactionBinding;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.platform.gateway.exception.GatewayCsrfTokenException;
import com.syncturtle.platform.gateway.filter.GatewayFilterOrders;
import com.syncturtle.platform.gateway.security.PassportAuthenticationToken;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfRoutePolicy;
import com.syncturtle.platform.gateway.security.csrf.GatewayCsrfTokenProcessor;
import com.syncturtle.platform.gateway.security.csrf.ValidatedPreAuthCsrfToken;
import com.syncturtle.platform.gateway.security.csrf.ValidatedSessionCsrfToken;
import com.syncturtle.platform.gateway.type.GatewayCsrfRequirement;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public final class CsrfMiddlewareFilter implements GlobalFilter, Ordered {

    private static final FormHttpMessageReader FORM_READER = new FormHttpMessageReader();
    private static final ResolvableType FORM_DATA_TYPE = ResolvableType.forClassWithGenerics(
            MultiValueMap.class,
            String.class,
            String.class);

    private final SecurityCookieFactory cookieFactory;
    private final GatewayCsrfTokenProcessor tokenProcessor;
    private final GatewayCsrfRoutePolicy routePolicy;

    @Override
    public int getOrder() {
        return GatewayFilterOrders.CSRF;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        GatewayCsrfRequirement requirement = routePolicy.requirement(
                request.getMethod(),
                request.getPath().pathWithinApplication().value());

        if (requirement == GatewayCsrfRequirement.NONE) {
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
                                requirement,
                                signedToken,
                                submittedFormToken(
                                        cachedRequest.getHeaders().getFirst(CSRF_HEADER_NAME),
                                        formData.getFirst(CSRF_FORM_FIELD_NAME))));
            });
        }

        String submittedToken = request.getHeaders().getFirst(CSRF_HEADER_NAME);
        return validateOrReject(exchange, chain, requirement, signedToken, submittedToken);
    }

    private Mono<Void> validateOrReject(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            GatewayCsrfRequirement requirement,
            String signedToken,
            String submittedToken) {
        try {
            if (requirement == GatewayCsrfRequirement.PREAUTH) {
                ValidatedPreAuthCsrfToken validated = tokenProcessor.validatePreAuth(signedToken, submittedToken);
                return forwardPreAuth(exchange, chain, validated);
            }

            ValidatedSessionCsrfToken validated = tokenProcessor.validateSession(signedToken, submittedToken);
            if (requirement == GatewayCsrfRequirement.TRANSPORT_SESSION) {
                return forwardTransportSession(exchange, chain, validated);
            }
            return requireAuthenticatedSession(exchange, chain, validated);
        } catch (GatewayCsrfTokenException exception) {
            return reject(exchange);
        }
    }

    private Mono<Void> forwardPreAuth(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            ValidatedPreAuthCsrfToken validated) {
        if (!routePolicy.isAdminHandoffStart(
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().pathWithinApplication().value())) {
            return chain.filter(exchange);
        }

        String binding = PreAuthTransactionBinding
                .fromValidatedSignedCsrfToken(validated.getSignedCookieToken())
                .getValue();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HDR_PREAUTH_TRANSACTION_BINDING, binding))
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private Mono<Void> forwardTransportSession(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            ValidatedSessionCsrfToken validated) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HDR_INTERNAL_CSRF_SESSION_ID, validated.getSessionId()))
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private Mono<Void> requireAuthenticatedSession(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            ValidatedSessionCsrfToken validated) {
        return exchange.getPrincipal()
                .ofType(PassportAuthenticationToken.class)
                .filter(authentication -> validated.getSessionId()
                        .equals(authentication.getValidatedSession().getSessionId()))
                .hasElement()
                .flatMap(matchesSession -> {
                    if (!matchesSession) {
                        return reject(exchange);
                    }
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        cookieFactory.clearCsrfCookies().forEach(exchange.getResponse()::addCookie);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = """
                {"ok":false,"error":"INVALID_CSRF_TOKEN","message":"Invalid CSRF token."}
                """.getBytes(StandardCharsets.UTF_8);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
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
