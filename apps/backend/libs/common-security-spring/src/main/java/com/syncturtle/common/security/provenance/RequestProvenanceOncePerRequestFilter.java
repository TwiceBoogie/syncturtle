package com.syncturtle.common.security.provenance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.exception.RequestProvenanceException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class RequestProvenanceOncePerRequestFilter extends OncePerRequestFilter {

    public static final String VERIFIED_CLAIMS_ATTRIBUTE = RequestProvenanceOncePerRequestFilter.class.getName()
            + ".verifiedClaims";
    private static final byte[] UNAUTHORIZED_RESPONSE = "{\"code\":\"UNTRUSTED_REQUEST_PROVENANCE\"}"
            .getBytes(StandardCharsets.UTF_8);

    private final RequestProvenanceVerifier verifier;
    private final RequestProvenanceRoutePolicy routePolicy;
    private final RequestProvenanceCanonicalizer canonicalizer;

    public RequestProvenanceOncePerRequestFilter(
            RequestProvenanceVerifier verifier,
            RequestProvenanceRoutePolicy routePolicy,
            RequestProvenanceCanonicalizer canonicalizer) {
        Assert.notNull(verifier, "verifier is required");
        Assert.notNull(routePolicy, "routePolicy is required");
        Assert.notNull(canonicalizer, "canonicalizer is required");

        this.verifier = verifier;
        this.routePolicy = routePolicy;
        this.canonicalizer = canonicalizer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RequestHeaderSource headers = name -> Collections.list(request.getHeaders(name));

        final boolean exempt;
        try {
            exempt = routePolicy.isExempt(request.getMethod(), request.getRequestURI());
        } catch (RequestProvenanceException exception) {
            reject(response);
            return;
        }

        if (exempt) {
            try {
                rejectProtectedHeadersOnExemptRequest(request, headers);
            } catch (RequestProvenanceException exception) {
                reject(response);
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        try {
            String assertion = requireSingleAssertion(request);
            RequestProvenanceClaims claims = verifier.verify(
                    assertion,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    headers);

            routePolicy.authorize(
                    claims,
                    request.getMethod(),
                    request.getRequestURI(),
                    headers);

            request.setAttribute(VERIFIED_CLAIMS_ATTRIBUTE, claims);
        } catch (RequestProvenanceException exception) {
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void rejectProtectedHeadersOnExemptRequest(
            HttpServletRequest request,
            RequestHeaderSource headers) {
        if (request.getHeaders(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE).hasMoreElements()
                || canonicalizer.hasAnyProtectedHeader(headers)) {
            throw new RequestProvenanceException(
                    "protected request provenance headers are not allowed on exempt routes");
        }
    }

    private static String requireSingleAssertion(HttpServletRequest request) {
        List<String> assertions = Collections.list(request.getHeaders(GatewayHeaders.HDR_INTERNAL_REQUEST_PROVENANCE));
        if (assertions.size() != 1) {
            throw new RequestProvenanceException("exactly one request provenance assertion is required");
        }

        return assertions.getFirst();
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(UNAUTHORIZED_RESPONSE.length);
        response.getOutputStream().write(UNAUTHORIZED_RESPONSE);
        response.flushBuffer();
    }

}
