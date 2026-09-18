package com.syncturtle.common.security.provenance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.exception.RequestProvenanceException;
import com.syncturtle.common.security.type.ActorHeaderMode;

public final class RequestProvenanceRoutePolicy {

    private final List<CompiledProtectedRoute> protectedRoutes;
    private final List<CompiledExemptRoute> exemptRoutes;
    private final RequestProvenanceCanonicalizer canonicalizer;

    public RequestProvenanceRoutePolicy(
            RequestProvenancePolicy policy,
            RequestProvenanceCanonicalizer canonicalizer) {
        Assert.notNull(policy, "policy is required");
        Assert.notNull(canonicalizer, "canonicalizer is required");

        PathPatternParser parser = new PathPatternParser();
        this.protectedRoutes = compileProtectedRoutes(policy.getInboundRoutes(), parser);
        this.exemptRoutes = compileExemptRoutes(policy.getExemptRoutes(), parser);
        this.canonicalizer = canonicalizer;
    }

    public boolean isExempt(String method, String rawPath) {
        HttpMethod requestMethod = requestMethod(method);
        PathContainer path = requestPath(rawPath);

        for (CompiledExemptRoute route : exemptRoutes) {
            if (route.matches(requestMethod, path)) {
                return true;
            }
        }

        return false;
    }

    public void authorize(
            RequestProvenanceClaims claims,
            String method,
            String rawPath,
            RequestHeaderSource headers) {
        Assert.notNull(claims, "request provenance claims are required");
        Assert.notNull(headers, "request header source is required");

        HttpMethod requestMethod = requestMethod(method);
        PathContainer path = requestPath(rawPath);

        CompiledProtectedRoute match = null;
        for (CompiledProtectedRoute route : protectedRoutes) {
            if (!route.matches(requestMethod, path)) {
                continue;
            }

            if (match != null) {
                throw new IllegalStateException("ambiguous request provenance route");
            }

            match = route;
        }

        if (match == null || !match.issuers.contains(claims.getIssuer())) {
            throw invalid("request provenance route is unauthorized");
        }

        boolean actorPresent = !safeValues(headers.values(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID)).isEmpty();
        if (match.actorHeaderMode == ActorHeaderMode.REQUIRED && !actorPresent) {
            throw invalid("request provenance actor is required");
        }
        if (match.actorHeaderMode == ActorHeaderMode.FORBIDDEN && actorPresent) {
            throw invalid("request provenance actor is unauthorized");
        }
        if (!match.trustedHeadersAllowed && canonicalizer.hasNonActorProtectedHeader(headers)) {
            throw invalid("request provenance trusted headers are unauthorized");
        }
    }

    private HttpMethod requestMethod(String method) {
        String canonicalMethod = canonicalizer.canonicalMethod(method);
        try {
            return HttpMethod.valueOf(canonicalMethod);
        } catch (IllegalArgumentException exception) {
            throw invalid("request method is unsupported", exception);
        }
    }

    private PathContainer requestPath(String rawPath) {
        try {
            return PathContainer.parsePath(canonicalizer.canonicalRawPath(rawPath));
        } catch (RequestProvenanceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid("raw request path is invalid", exception);
        }
    }

    private static List<CompiledProtectedRoute> compileProtectedRoutes(
            List<RequestProvenancePolicy.InboundRoute> configured,
            PathPatternParser parser) {
        List<CompiledProtectedRoute> routes = new ArrayList<>(configured.size());
        for (RequestProvenancePolicy.InboundRoute route : configured) {
            routes.add(new CompiledProtectedRoute(
                    route.getMethods(),
                    parser.parse(route.getPathPattern()),
                    route.getIssuerIds(),
                    route.getActorHeaderMode(),
                    route.isTrustedHeadersAllowed()));
        }

        return List.copyOf(routes);
    }

    private static List<CompiledExemptRoute> compileExemptRoutes(
            List<RequestProvenancePolicy.ExemptRoute> configured,
            PathPatternParser parser) {
        List<CompiledExemptRoute> routes = new ArrayList<>(configured.size());
        for (RequestProvenancePolicy.ExemptRoute route : configured) {
            routes.add(new CompiledExemptRoute(
                    route.getMethods(),
                    parser.parse(route.getPathPattern())));
        }

        return List.copyOf(routes);
    }

    private static List<String> safeValues(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static RequestProvenanceException invalid(String message) {
        return new RequestProvenanceException(message);
    }

    private static RequestProvenanceException invalid(String message, Throwable cause) {
        return new RequestProvenanceException(message, cause);
    }

    private static final class CompiledProtectedRoute {

        private final Set<HttpMethod> methods;
        private final PathPattern path;
        private final Set<String> issuers;
        private final ActorHeaderMode actorHeaderMode;
        private final boolean trustedHeadersAllowed;

        private CompiledProtectedRoute(
                Set<HttpMethod> methods,
                PathPattern path,
                Set<String> issuers,
                ActorHeaderMode actorHeaderMode,
                boolean trustedHeadersAllowed) {
            this.methods = methods;
            this.path = path;
            this.issuers = issuers;
            this.actorHeaderMode = actorHeaderMode;
            this.trustedHeadersAllowed = trustedHeadersAllowed;
        }

        private boolean matches(HttpMethod method, PathContainer candidate) {
            return methods.contains(method) && path.matches(candidate);
        }

    }

    private static final class CompiledExemptRoute {
        private final Set<HttpMethod> methods;
        private final PathPattern path;

        private CompiledExemptRoute(Set<HttpMethod> methods, PathPattern path) {
            this.methods = methods;
            this.path = path;
        }

        private boolean matches(HttpMethod method, PathContainer candidate) {
            return methods.contains(method) && path.matches(candidate);
        }
    }

}
