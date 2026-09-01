package com.syncturtle.platform.gateway.security;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.platform.gateway.type.GatewayRouteSecurityCategory;

@Component
public final class GatewayRouteSecurityPolicy {

    private static final String RETIRED_INSTANCE_ADMIN_SIGN_OUT_PATH = "/api/instances/admins/sign-out";
    private static final String USER_SESSION_PATH = "/api/users/me/sessions";

    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/auth/email-check",
            "/auth/magic-generate",
            "/auth/sign-in",
            "/auth/sign-up",
            "/auth/magic-sign-in",
            "/auth/magic-sign-up",
            "/auth/forgot-password",
            "/auth/refresh",
            "/auth/sign-out",
            "/auth/admin/sign-out",
            "/api/instances/admins/sign-in",
            "/api/instances/admins/sign-up");

    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
            "/auth/admin/session",
            "/auth/google",
            "/auth/google/callback",
            "/auth/github",
            "/auth/github/callback",
            "/auth/gitlab",
            "/auth/gitlab/callback",
            "/actuator/info");

    public GatewayRouteSecurityCategory classify(ServerWebExchange exchange) {
        if (exchange == null) {
            return GatewayRouteSecurityCategory.DEFAULT_DENY;
        }

        return classify(
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().pathWithinApplication().value());
    }

    public GatewayRouteSecurityCategory classify(HttpMethod method, String rawPath) {
        if (method == null || !StringUtils.hasText(rawPath)) {
            return GatewayRouteSecurityCategory.DEFAULT_DENY;
        }

        String path = normalizePath(rawPath);

        if (RETIRED_INSTANCE_ADMIN_SIGN_OUT_PATH.equals(path)) {
            return GatewayRouteSecurityCategory.DEFAULT_DENY;
        }

        if (isUserSessionNamespace(path)) {
            return classifyUserSessionRoute(method, path);
        }

        if (HttpMethod.OPTIONS.equals(method)) {
            return isKnownPath(path)
                    ? GatewayRouteSecurityCategory.PUBLIC
                    : GatewayRouteSecurityCategory.DEFAULT_DENY;
        }

        if (isPublic(method, path)) {
            return GatewayRouteSecurityCategory.PUBLIC;
        }

        if (isOptionalAuthentication(method, path)) {
            return GatewayRouteSecurityCategory.OPTIONAL_AUTH;
        }

        if (isProtected(path)) {
            return GatewayRouteSecurityCategory.PROTECTED;
        }

        return GatewayRouteSecurityCategory.DEFAULT_DENY;
    }

    public boolean requiresInstanceAdmin(ServerWebExchange exchange) {
        if (classify(exchange) != GatewayRouteSecurityCategory.PROTECTED) {
            return false;
        }

        String path = normalizePath(exchange.getRequest().getPath().pathWithinApplication().value());
        return path.equals("/api/instances") || path.startsWith("/api/instances/");
    }

    public ServerWebExchangeMatcher matcher(GatewayRouteSecurityCategory category) {
        return exchange -> classify(exchange) == category
                ? ServerWebExchangeMatcher.MatchResult.match()
                : ServerWebExchangeMatcher.MatchResult.notMatch();
    }

    private static boolean isPublic(HttpMethod method, String path) {
        if (HttpMethod.POST.equals(method)) {
            return PUBLIC_POST_PATHS.contains(path) || path.startsWith("/auth/reset-password/");
        }

        if (HttpMethod.GET.equals(method)) {
            return PUBLIC_GET_PATHS.contains(path)
                    || path.equals("/api/instances")
                    || path.equals("/actuator/health")
                    || path.startsWith("/actuator/health/");
        }

        return HttpMethod.HEAD.equals(method)
                && (path.equals("/api/instances")
                        || path.equals("/actuator/health")
                        || path.startsWith("/actuator/health/"));
    }

    private static boolean isOptionalAuthentication(HttpMethod method, String path) {
        return (HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method))
                && (path.equals("/api/instances/admins/session")
                        || path.equals("/api/get-csrf-token"));
    }

    private static boolean isProtected(String path) {
        return path.equals("/auth/set-password")
                || path.equals("/api/instances")
                || path.startsWith("/api/instances/")
                || path.equals("/api/users")
                || path.startsWith("/api/users/")
                || path.equals("/api/workspaces")
                || path.startsWith("/api/workspaces/")
                || path.equals("/api/assets")
                || path.startsWith("/api/assets/");
    }

    private static GatewayRouteSecurityCategory classifyUserSessionRoute(HttpMethod method, String path) {
        boolean knownPath = isKnownUserSessionPath(path);
        if (HttpMethod.OPTIONS.equals(method)) {
            return knownPath ? GatewayRouteSecurityCategory.PUBLIC : GatewayRouteSecurityCategory.DEFAULT_DENY;
        }

        if (HttpMethod.GET.equals(method) && USER_SESSION_PATH.equals(path)) {
            return GatewayRouteSecurityCategory.PROTECTED;
        }

        if (HttpMethod.DELETE.equals(method) && knownPath) {
            return GatewayRouteSecurityCategory.PROTECTED;
        }

        return GatewayRouteSecurityCategory.DEFAULT_DENY;
    }

    private static boolean isKnownUserSessionPath(String path) {
        if (USER_SESSION_PATH.equals(path) || (USER_SESSION_PATH + "/others").equals(path)) {
            return true;
        }

        if (!path.startsWith(USER_SESSION_PATH + "/")) {
            return false;
        }

        String sessionId = path.substring(USER_SESSION_PATH.length() + 1);
        return isCanonicalUuid(sessionId);
    }

    private static boolean isUserSessionNamespace(String path) {
        return path.startsWith("/api/users/me/session");
    }

    private static boolean isCanonicalUuid(String value) {
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isKnownPath(String path) {
        return PUBLIC_POST_PATHS.contains(path)
                || PUBLIC_GET_PATHS.contains(path)
                || path.startsWith("/auth/reset-password/")
                || path.equals("/api/instances")
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.equals("/api/instances/admins/session")
                || path.equals("/api/get-csrf-token")
                || isProtected(path);
    }

    private static String normalizePath(String rawPath) {
        String path = rawPath.trim();
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

}
