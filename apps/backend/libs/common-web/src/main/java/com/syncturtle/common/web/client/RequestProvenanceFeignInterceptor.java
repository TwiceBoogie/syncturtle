package com.syncturtle.common.web.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.exception.RequestProvenanceException;
import com.syncturtle.common.security.provenance.RequestHeaderSource;
import com.syncturtle.common.security.provenance.RequestProvenanceCanonicalizer;
import com.syncturtle.common.security.provenance.RequestProvenanceOutboundPolicy;
import com.syncturtle.common.security.provenance.RequestProvenanceSigner;
import com.syncturtle.common.security.type.ActorHeaderMode;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public final class RequestProvenanceFeignInterceptor implements RequestInterceptor {

    private final RequestProvenanceSigner signer;
    private final RequestProvenanceOutboundPolicy policy;
    private final RequestProvenanceCanonicalizer canonicalizer;

    public RequestProvenanceFeignInterceptor(
            RequestProvenanceSigner signer,
            RequestProvenanceOutboundPolicy policy,
            RequestProvenanceCanonicalizer canonicalizer) {
        Assert.notNull(signer, "signer is required");
        Assert.notNull(policy, "policy is required");
        Assert.notNull(canonicalizer, "canonicalizer is required");

        this.signer = signer;
        this.policy = policy;
        this.canonicalizer = canonicalizer;
    }

    @Override
    public void apply(RequestTemplate template) {
        Assert.notNull(template, "template is required");
        Assert.notNull(template.feignTarget(), "Feign target is required");

        ResolvedTarget resolvedTarget = resolveTarget(template);
        RequestProvenanceOutboundPolicy.AuthorizedOutboundRoute route = policy.authorize(template.feignTarget().name(),
                template.method(), resolvedTarget.rawPath);
        List<String> explicitActorValues = headerValues(template.headers(), GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID);

        stripProtectedHeaders(template);
        applyActor(template, route.getActorMode(), explicitActorValues);

        RequestHeaderSource headers = name -> headerValues(template.headers(), name);

        String assertion = signer.sign(route.getAudience(), template.method(), resolvedTarget.rawPath,
                resolvedTarget.rawQuery, headers);

        template.header(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE, assertion);
    }

    private void stripProtectedHeaders(RequestTemplate template) {
        template.removeHeader(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE);

        for (String header : canonicalizer.protectedHeaderNames()) {
            template.removeHeader(header);
        }
    }

    private static void applyActor(RequestTemplate template, ActorHeaderMode actorMode,
            List<String> explicitActorValues) {
        if (actorMode == ActorHeaderMode.REQUIRED) {
            template.header(GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID, requireActor(explicitActorValues));
            return;
        }

        if (!explicitActorValues.isEmpty()) {
            throw new IllegalStateException("actor header supplied for a request provenance route that forbids actors");
        }
    }

    private static String requireActor(List<String> values) {
        if (values.size() != 1) {
            throw new RequestProvenanceException("actor-bound internal call requires exactly one actor user id");
        }

        String value = values.getFirst();

        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new RequestProvenanceException("actor-bound internal call requires a canonical actor user id");
        }

        return value;
    }

    private static ResolvedTarget resolveTarget(RequestTemplate template) {
        RequestTemplate targeted = RequestTemplate.from(template);
        URI requestUri = URI.create(targeted.url());

        if (!requestUri.isAbsolute()) {
            targeted.target(template.feignTarget().url());
            requestUri = URI.create(targeted.url());
        }

        return new ResolvedTarget(requestUri.getRawPath(), requestUri.getRawQuery());
    }

    private static List<String> headerValues(Map<String, Collection<String>> headers, String name) {
        List<String> matches = new ArrayList<>();

        for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(name)) {
                continue;
            }

            Collection<String> values = entry.getValue();
            if (values != null) {
                matches.addAll(values);
            }
        }

        return List.copyOf(matches);
    }

    private static final class ResolvedTarget {

        private final String rawPath;
        private final String rawQuery;

        private ResolvedTarget(
                String rawPath,
                String rawQuery) {
            this.rawPath = rawPath;
            this.rawQuery = rawQuery;
        }
    }

}
