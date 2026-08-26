package com.syncturtle.platform.gateway.security;

import java.util.List;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

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
public final class CookieOrBearerServerAuthenticationConverter implements ServerAuthenticationConverter {

    private final ServerBearerTokenAuthenticationConverter bearerTokenConverter;
    private final SecurityCookieFactory cookieFactory;

    public CookieOrBearerServerAuthenticationConverter(SecurityCookieFactory cookieFactory) {
        this.bearerTokenConverter = new ServerBearerTokenAuthenticationConverter();
        this.cookieFactory = cookieFactory;
    }

    /**
     * Performs conditional extraction logic.
     * 
     * <p>
     * Executes the {@code bearerTokenConverter} first. If the resulting
     * {@link Mono} is empty,
     * it switches to {@link #convertCookie(ServerWebExchange)}.
     * 
     * @param exchange the current reactive exchange
     * @return a {@link Mono} containing the first valid {@link Authentication}
     *         found
     */
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        Mono<Authentication> cookieAuthentication = Mono.defer(() -> convertCookie(exchange));
        List<String> authorizationHeaders = exchange.getRequest()
                .getHeaders()
                .getOrEmpty(HttpHeaders.AUTHORIZATION);

        if (authorizationHeaders.isEmpty()) {
            return cookieAuthentication;
        }
        if (authorizationHeaders.size() != 1 || !StringUtils.hasText(authorizationHeaders.getFirst())) {
            return Mono.error(invalidCredential());
        }

        return bearerTokenConverter.convert(exchange)
                .switchIfEmpty(Mono.error(invalidCredential()))
                .flatMap(headerAuthentication -> cookieAuthentication
                        .map(cookie -> requireSameCredential(headerAuthentication, cookie))
                        .defaultIfEmpty(headerAuthentication));
    }

    /**
     * Internal helper to extract the JWT from the cookie store.
     * 
     * @param exchange the current exchange
     * @return a {@link BearerTokenAuthenticationToken} if the cookie exists, else
     *         {@link Mono#empty()}
     */
    private Mono<Authentication> convertCookie(ServerWebExchange exchange) {
        List<HttpCookie> cookies = exchange.getRequest()
                .getCookies()
                .get(cookieFactory.accessCookieName());

        if (cookies == null || cookies.isEmpty()) {
            return Mono.empty();
        }
        if (cookies.size() != 1) {
            return Mono.error(conflictingCredentials());
        }

        String value = cookies.getFirst().getValue();
        if (!StringUtils.hasText(value)) {
            return Mono.empty();
        }

        return Mono.just(new BearerTokenAuthenticationToken(value.trim()));
    }

    private Authentication requireSameCredential(
            Authentication headerAuthentication,
            Authentication cookieAuthentication) {
        String headerToken = tokenValue(headerAuthentication);
        String cookieToken = tokenValue(cookieAuthentication);

        if (!headerToken.equals(cookieToken)) {
            throw conflictingCredentials();
        }

        return headerAuthentication;
    }

    private String tokenValue(Authentication authentication) {
        if (authentication instanceof BearerTokenAuthenticationToken bearerToken) {
            return bearerToken.getToken();
        }
        throw conflictingCredentials();
    }

    private PassportAuthenticationException conflictingCredentials() {
        return new PassportAuthenticationException(PassportAuthenticationFailureReason.CREDENTIAL_CONFLICT);
    }

    private PassportAuthenticationException invalidCredential() {
        return new PassportAuthenticationException(PassportAuthenticationFailureReason.CREDENTIAL_INVALID);
    }

}
