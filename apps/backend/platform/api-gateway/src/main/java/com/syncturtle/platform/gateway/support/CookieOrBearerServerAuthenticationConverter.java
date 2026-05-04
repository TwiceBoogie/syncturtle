package com.syncturtle.platform.gateway.support;

import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_ACCESS_TOKEN;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * A fallback authentication converter that extracts JWTs from cookies only if
 * the standard {@code Authorization} header is missing.
 * 
 * <p>
 * This implementation follows a "Header-First" strategy:
 * <ol>
 * <li>It delegates to a primary converter (e.g.,
 * {@code ServerBearerTokenAuthenticationConverter}).</li>
 * <li>If the primary converter returns empty (no header found), this class
 * attempts
 * to locate the JWT within the cookie.</li>
 * </ol>
 * 
 * <p>
 * <b>Design Intent:</b> This allows support for both programmatic API access
 * (headers)
 * and browser-based sessions (Secure/HttpOnly cookies) within a single security
 * chain.
 * 
 * @author Salvador Sebastian
 * @see ServerAuthenticationConverter
 */
@Component
@RequiredArgsConstructor
public class CookieOrBearerServerAuthenticationConverter implements ServerAuthenticationConverter {

    private final ServerBearerTokenAuthenticationConverter delegate = new ServerBearerTokenAuthenticationConverter();

    /**
     * Performs conditional extraction logic.
     * 
     * <p>
     * Executes the {@code delegate} first. If the resulting {@link Mono} is empty,
     * it switches to {@link #extractFromCookie(ServerWebExchange)}.
     * 
     * @param exchange the current reactive exchange
     * @return a {@link Mono} containing the first valid {@link Authentication}
     *         found
     */
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return delegate.convert(exchange)
                .switchIfEmpty(Mono.defer(() -> extractFromCookie(exchange)));
    }

    /**
     * Internal helper to extract the JWT from the cookie store.
     * 
     * @param exchange the current exchange
     * @return a {@link BearerTokenAuthenticationToken} if the cookie exists, else
     *         {@link Mono#empty()}
     */
    private Mono<Authentication> extractFromCookie(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getCookies().getFirst(COOKIE_NAME_ACCESS_TOKEN))
                .map(cookie -> new BearerTokenAuthenticationToken(cookie.getValue()));
    }

}
