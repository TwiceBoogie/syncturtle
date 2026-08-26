package com.syncturtle.platform.gateway.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public final class GatewayRouteAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private static final String INSTANCE_ADMIN_AUTHORITY = "ROLE_INSTANCE_ADMIN";

    private final GatewayRouteSecurityPolicy routeSecurityrPolicy;

    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication
                .filter(Authentication::isAuthenticated)
                .<AuthorizationResult>map(current -> new AuthorizationDecision(isAuthorized(current, context)))
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    private boolean isAuthorized(Authentication authentication, AuthorizationContext context) {
        if (!routeSecurityrPolicy.requiresInstanceAdmin(context.getExchange())) {
            return true;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> INSTANCE_ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }

}
