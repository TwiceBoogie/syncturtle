package com.syncturtle.common.security.provenance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import com.syncturtle.common.security.exception.RequestProvenanceException;
import com.syncturtle.common.security.type.ActorHeaderMode;

public final class RequestProvenanceOutboundPolicy {

    private final List<CompiledOutboundRoute> routes;
    private final RequestProvenanceCanonicalizer canonicalizer;

    public RequestProvenanceOutboundPolicy(
            RequestProvenancePolicy policy,
            RequestProvenanceCanonicalizer canonicalizer) {
        Assert.notNull(policy, "policy is required");
        Assert.notNull(canonicalizer, "canonicalizer is required");

        PathPatternParser parser = new PathPatternParser();
        List<CompiledOutboundRoute> compiled = new ArrayList<>(policy.getOutboundRoutes().size());
        for (RequestProvenancePolicy.OutboundRoute route : policy.getOutboundRoutes()) {
            compiled.add(new CompiledOutboundRoute(
                    route.getClientName(),
                    route.getAudience(),
                    route.getMethods(),
                    parser.parse(route.getPathPattern()),
                    route.getActorHeaderMode()));
        }

        this.routes = List.copyOf(compiled);
        this.canonicalizer = canonicalizer;
    }

    public AuthorizedOutboundRoute authorize(String clientName, String method, String rawPath) {
        Assert.hasText(clientName, "clientName is required");

        HttpMethod requestMethod = requestMethod(method);
        PathContainer path = requestPath(rawPath);

        CompiledOutboundRoute match = null;
        for (CompiledOutboundRoute route : routes) {
            if (!route.matches(clientName, requestMethod, path)) {
                continue;
            }

            if (match != null) {
                throw new IllegalStateException("ambiguous outbound request provenance route");
            }

            match = route;
        }

        if (match == null) {
            throw new RequestProvenanceException(
                    "outbound request provenance route is unauthorized");
        }

        return new AuthorizedOutboundRoute(match.audience, match.actorHeaderMode);
    }

    private HttpMethod requestMethod(String method) {
        String canonicalMethod = canonicalizer.canonicalMethod(method);
        try {
            return HttpMethod.valueOf(canonicalMethod);
        } catch (IllegalArgumentException exception) {
            throw new RequestProvenanceException("request method is unsupported", exception);
        }
    }

    private PathContainer requestPath(String rawPath) {
        try {
            return PathContainer.parsePath(canonicalizer.canonicalRawPath(rawPath));
        } catch (RequestProvenanceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RequestProvenanceException("raw request path is invalid", exception);
        }
    }

    private static final class CompiledOutboundRoute {

        private final String clientName;
        private final String audience;
        private final Set<HttpMethod> methods;
        private final PathPattern path;
        private final ActorHeaderMode actorHeaderMode;

        private CompiledOutboundRoute(
                String clientName,
                String audience,
                Set<HttpMethod> methods,
                PathPattern path,
                ActorHeaderMode actorHeaderMode) {
            this.clientName = clientName;
            this.audience = audience;
            this.methods = methods;
            this.path = path;
            this.actorHeaderMode = actorHeaderMode;
        }

        private boolean matches(
                String candidateClient,
                HttpMethod candidateMethod,
                PathContainer candidatePath) {
            return clientName.equals(candidateClient)
                    && methods.contains(candidateMethod)
                    && path.matches(candidatePath);
        }

    }

    public static final class AuthorizedOutboundRoute {

        private final String audience;
        private final ActorHeaderMode actorMode;

        private AuthorizedOutboundRoute(String audience, ActorHeaderMode actorMode) {
            this.audience = audience;
            this.actorMode = actorMode;
        }

        public String getAudience() {
            return audience;
        }

        public ActorHeaderMode getActorMode() {
            return actorMode;
        }

    }

}
