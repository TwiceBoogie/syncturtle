package com.syncturtle.platform.gateway.filter.global;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.platform.gateway.filter.GatewayFilterOrders;
import com.syncturtle.platform.gateway.security.GatewayRouteSecurityPolicy;
import com.syncturtle.platform.gateway.security.browser.GatewayBrowserOriginPolicy;
import com.syncturtle.platform.gateway.type.GatewayRouteSecurityCategory;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public final class BrowserRequestPolicyFilter implements GlobalFilter, Ordered {

    private static final Set<String> NATIVE_FORM_PATHS = Set.of(
            "/auth/sign-in", "/auth/sign-up", "/auth/magic-sign-in", "/auth/magic-sign-up",
            "/api/instances/admins/sign-in", "/api/instances/admins/sign-up");
    private static final Set<String> OAUTH_PROVIDERS = Set.of("google", "github", "gitlab");

    private final GatewayBrowserOriginPolicy origins;
    private final GatewayRouteSecurityPolicy routes;
    private final SecurityCookieFactory cookies;
    private final SecureTokenGenerator tokens;

    @Override
    public int getOrder() {
        return GatewayFilterOrders.BROWSER_REQUEST_POLICY;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Authentication entry-point responses bypass Gateway routing. Routed services cannot
        // claim the Gateway-only rejection marker used to permit a bounded browser replay.
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().remove(GatewayHeaders.HDR_GATEWAY_AUTH_REJECTION);
            return Mono.empty();
        });
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = normalize(request.getPath().pathWithinApplication().value());

        if (unsafe(method) && !bearerOnlyProtectedRequest(exchange)) {
            if (!origins.accepts(exchange)) {
                return reject(exchange);
            }
            if (!nativeForm(method, path, request)
                    && !List.of("1").equals(request.getHeaders().get(GatewayHeaders.HDR_BROWSER_REQUEST))) {
                return reject(exchange);
            }
        }

        if (nativeForm(method, path, request) && path.startsWith("/api/instances/admins/")) {
            String secret = tokens.generateBase64Url(32);
            exchange.getResponse().addCookie(cookies.adminHandoffBindingCookie(secret, Duration.ofSeconds(30)));
            return forwardBinding(exchange, chain, GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING, secret);
        }
        if (HttpMethod.GET.equals(method) && "/auth/admin/session".equals(path)) {
            return forwardExistingBinding(exchange, chain, GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING,
                    cookies.adminHandoffBindingCookieName());
        }
        if (HttpMethod.GET.equals(method) && oauthInitiation(path)) {
            String secret = tokens.generateBase64Url(32);
            exchange.getResponse().addCookie(cookies.oauthBindingCookie(secret, Duration.ofMinutes(10)));
            return forwardBinding(exchange, chain, GatewayHeaders.HDR_OAUTH_BROWSER_BINDING, secret);
        }
        if (HttpMethod.GET.equals(method) && oauthCallback(path)) {
            return forwardExistingBinding(exchange, chain, GatewayHeaders.HDR_OAUTH_BROWSER_BINDING,
                    cookies.oauthBindingCookieName());
        }
        return chain.filter(exchange);
    }

    private boolean bearerOnlyProtectedRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        if (routes.classify(exchange) != GatewayRouteSecurityCategory.PROTECTED
                || request.getHeaders().containsHeader(HttpHeaders.ORIGIN)
                || !request.getCookies().isEmpty()) {
            return false;
        }
        List<String> authorization = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
        return authorization.size() == 1
                && authorization.getFirst().startsWith("Bearer ")
                && !authorization.getFirst().substring(7).isBlank();
    }

    private Mono<Void> forwardExistingBinding(ServerWebExchange exchange, GatewayFilterChain chain,
            String header, String cookieName) {
        List<HttpCookie> values = exchange.getRequest().getCookies().get(cookieName);
        if (values == null || values.size() != 1 || values.getFirst().getValue().isBlank()) {
            return chain.filter(exchange);
        }
        return forwardBinding(exchange, chain, header, values.getFirst().getValue());
    }

    private Mono<Void> forwardBinding(ServerWebExchange exchange, GatewayFilterChain chain,
            String header, String secret) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(header, hash(secret)))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private static String hash(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean nativeForm(HttpMethod method, String path, ServerHttpRequest request) {
        MediaType contentType = request.getHeaders().getContentType();
        return HttpMethod.POST.equals(method)
                && (NATIVE_FORM_PATHS.contains(path) || path.startsWith("/auth/reset-password/"))
                && contentType != null && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType);
    }

    private static boolean oauthInitiation(String path) {
        return path.startsWith("/auth/") && OAUTH_PROVIDERS.contains(path.substring(6));
    }

    private static boolean oauthCallback(String path) {
        for (String provider : OAUTH_PROVIDERS) {
            if (path.equals("/auth/" + provider + "/callback")) return true;
        }
        return false;
    }

    private static boolean unsafe(HttpMethod method) {
        return !HttpMethod.GET.equals(method) && !HttpMethod.HEAD.equals(method)
                && !HttpMethod.OPTIONS.equals(method);
    }

    private static String normalize(String path) {
        while (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }

    private static Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"ok\":false,\"error\":\"BROWSER_REQUEST_REJECTED\"}".getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
