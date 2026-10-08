package com.syncturtle.common.security.provenance;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;

import org.springframework.util.Assert;

import com.syncturtle.common.security.exception.RequestProvenanceException;

public final class RequestProvenanceVerifier {

    private final RequestProvenanceAssertionSerializer serializer;
    private final RequestProvenanceCanonicalizer canonicalizer;
    private final RequestProvenanceVerificationKeys verificationKeys;
    private final String audience;
    private final Clock clock;

    public RequestProvenanceVerifier(
            RequestProvenanceAssertionSerializer serializer,
            RequestProvenanceCanonicalizer canonicalizer,
            RequestProvenanceVerificationKeys verificationKeys,
            RequestProvenancePolicy policy,
            Clock clock) {
        Assert.notNull(serializer, "serializer is required");
        Assert.notNull(canonicalizer, "canonicalizer is required");
        Assert.notNull(verificationKeys, "verificationKeys are required");
        Assert.notNull(policy, "policy is required");
        Assert.notNull(clock, "clock is required");

        this.serializer = serializer;
        this.canonicalizer = canonicalizer;
        this.verificationKeys = verificationKeys;
        this.audience = policy.getWorkloadId();
        this.clock = clock;
    }

    public RequestProvenanceClaims verify(
            String assertion,
            String method,
            String rawPath,
            String rawQuery,
            RequestHeaderSource headers) {
        RequestProvenanceAssertionSerializer.DecodedAssertion decoded = serializer.decode(assertion);
        RequestProvenanceClaims claims = decoded.getClaims();

        PublicKey publicKey = verificationKeys.require(claims.getIssuer(), claims.getKeyId());
        verifySignature(publicKey, decoded);

        if (!audience.equals(claims.getAudience())) {
            throw invalid("request provenance audience does not match");
        }

        if (!canonicalizer.canonicalMethod(method).equals(claims.getMethod())) {
            throw invalid("request provenance method does not match");
        }

        if (!canonicalizer.canonicalRawPath(rawPath).equals(claims.getRawPath())) {
            throw invalid("request provenance path does not match");
        }

        if (!RequestProvenanceCanonicalizer.constantTimeEquals(
                canonicalizer.digestRawQuery(rawQuery),
                claims.getRawQueryDigest())) {
            throw invalid("request provenance query does not match");
        }

        if (!RequestProvenanceCanonicalizer.constantTimeEquals(
                canonicalizer.digestProtectedHeaders(headers),
                claims.getProtectedHeaderDigest())) {
            throw invalid("request provenance protected headers do not match");
        }

        validateTime(claims);

        return claims;
    }

    private static void verifySignature(
            PublicKey publicKey,
            RequestProvenanceAssertionSerializer.DecodedAssertion decoded) {
        try {
            Signature signature = Signature.getInstance(RequestProvenanceProtocol.ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(decoded.getPayload().getBytes(StandardCharsets.US_ASCII));

            if (!signature.verify(decoded.getSignature())) {
                throw invalid("request provenance signature is invalid");
            }
        } catch (GeneralSecurityException exception) {
            throw invalid("request provenance signature verification failed", exception);
        }
    }

    private void validateTime(RequestProvenanceClaims claims) {
        Instant now = Instant.now(clock);
        if (claims.getIssuedAt().isAfter(now.plus(RequestProvenanceProtocol.ALLOWED_CLOCK_SKEW))) {
            throw invalid("request provenance assertion is from the future");
        }

        if (claims.getExpiresAt().isBefore(now.minus(RequestProvenanceProtocol.ALLOWED_CLOCK_SKEW))) {
            throw invalid("request provenance assertion is expired");
        }
    }

    private static RequestProvenanceException invalid(String message) {
        return new RequestProvenanceException(message);
    }

    private static RequestProvenanceException invalid(String message, Throwable cause) {
        return new RequestProvenanceException(message, cause);
    }

}
