package com.syncturtle.common.security.provenance;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.util.Assert;

import com.syncturtle.common.security.exception.RequestProvenanceException;

public final class RequestProvenanceSigner {

    private final RequestProvenanceAssertionSerializer serializer;
    private final RequestProvenanceCanonicalizer canonicalizer;
    private final RequestProvenanceSigningKey signingKey;
    private final String issuer;
    private final Clock clock;

    public RequestProvenanceSigner(
            RequestProvenanceAssertionSerializer serializer,
            RequestProvenanceCanonicalizer canonicalizer,
            RequestProvenanceSigningKey signingKey,
            RequestProvenancePolicy policy,
            Clock clock) {
        Assert.notNull(serializer, "serializer is required");
        Assert.notNull(canonicalizer, "canonicalizer is required");
        Assert.notNull(signingKey, "signingKey is required");
        Assert.notNull(policy, "policy is required");
        Assert.notNull(clock, "clock is required");
        Assert.state(policy.isSigningRequired(), "workload does not support request provenance signing");

        this.serializer = serializer;
        this.canonicalizer = canonicalizer;
        this.signingKey = signingKey;
        this.issuer = policy.getWorkloadId();
        this.clock = clock;
    }

    public String sign(
            String audience,
            String method,
            String rawPath,
            String rawQuery,
            RequestHeaderSource headers) {
        Instant issuedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);

        RequestProvenanceClaims claims = RequestProvenanceClaims.issue(
                signingKey.getKeyId(),
                issuer,
                audience,
                issuedAt,
                canonicalizer.canonicalMethod(method),
                canonicalizer.canonicalRawPath(rawPath),
                canonicalizer.digestRawQuery(rawQuery),
                canonicalizer.digestProtectedHeaders(headers));

        String payload = serializer.encodePayload(claims);
        try {
            Signature signature = Signature.getInstance(RequestProvenanceProtocol.ALGORITHM);
            signature.initSign(signingKey.getPrivateKey());
            signature.update(payload.getBytes(StandardCharsets.US_ASCII));

            return serializer.compose(payload, signature.sign());
        } catch (GeneralSecurityException exception) {
            throw new RequestProvenanceException("request provenance signing failed", exception);
        }
    }

}
