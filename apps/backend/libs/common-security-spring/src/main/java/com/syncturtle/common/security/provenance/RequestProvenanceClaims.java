package com.syncturtle.common.security.provenance;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

import lombok.Getter;

@Getter
public final class RequestProvenanceClaims {

    private static final Pattern WORKLOAD_ID = Pattern.compile("[a-z][a-z0-9-]{1,62}");
    private static final Pattern KEY_ID = Pattern.compile("[A-Za-z0-9_-]{22}");
    private static final Pattern HTTP_METHOD = Pattern.compile("[A-Z]{3,12}");
    private static final Pattern SHA_256_DIGEST = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final String keyId;
    private final String issuer;
    private final String audience;
    private final Instant issuedAt;
    private final String method;
    private final String rawPath;
    private final String rawQueryDigest;
    private final String protectedHeaderDigest;

    private RequestProvenanceClaims(
            String keyId,
            String issuer,
            String audience,
            Instant issuedAt,
            String method,
            String rawPath,
            String rawQueryDigest,
            String protectedHeaderDigest) {
        this.keyId = requireMatch(keyId, KEY_ID, "keyId");
        this.issuer = requireMatch(issuer, WORKLOAD_ID, "issuer");
        this.audience = requireMatch(audience, WORKLOAD_ID, "audience");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt is required");
        this.method = requireMatch(method, HTTP_METHOD, "method");
        this.rawPath = requireCanonicalText(rawPath, "rawPath");
        this.rawQueryDigest = requireMatch(rawQueryDigest, SHA_256_DIGEST, "rawQueryDigest");
        this.protectedHeaderDigest = requireMatch(protectedHeaderDigest, SHA_256_DIGEST, "protectedHeaderDigest");
    }

    static RequestProvenanceClaims issue(
            String keyId,
            String issuer,
            String audience,
            Instant issuedAt,
            String method,
            String rawPath,
            String rawQueryDigest,
            String protectedHeaderDigest) {
        return new RequestProvenanceClaims(
                keyId,
                issuer,
                audience,
                issuedAt,
                method,
                rawPath,
                rawQueryDigest,
                protectedHeaderDigest);
    }

    static RequestProvenanceClaims decoded(
            int version,
            String algorithm,
            String keyId,
            String issuer,
            String audience,
            Instant issuedAt,
            Instant expiresAt,
            String method,
            String rawPath,
            String rawQueryDigest,
            String protectedHeaderDigest) {
        if (version != RequestProvenanceProtocol.VERSION) {
            throw new IllegalArgumentException("unsupported request provenance version");
        }

        if (!RequestProvenanceProtocol.ALGORITHM.equals(algorithm)) {
            throw new IllegalArgumentException("unsupported request provenance algorithm");
        }

        Instant requiredExpiresAt = Objects.requireNonNull(issuedAt, "issuedAt is required")
                .plus(RequestProvenanceProtocol.ASSERTION_LIFETIME);
        if (!requiredExpiresAt.equals(Objects.requireNonNull(expiresAt, "expiresAt is required"))) {
            throw new IllegalArgumentException("request provenance lifetime is invalid");
        }

        return new RequestProvenanceClaims(
                keyId,
                issuer,
                audience,
                issuedAt,
                method,
                rawPath,
                rawQueryDigest,
                protectedHeaderDigest);
    }

    public int getVersion() {
        return RequestProvenanceProtocol.VERSION;
    }

    public String getAlgorithm() {
        return RequestProvenanceProtocol.ALGORITHM;
    }

    public Instant getExpiresAt() {
        return issuedAt.plus(RequestProvenanceProtocol.ASSERTION_LIFETIME);
    }

    private static String requireMatch(String value, Pattern pattern, String field) {
        String canonical = requireCanonicalText(value, field);
        if (!pattern.matcher(canonical).matches()) {
            throw new IllegalArgumentException(field + " has an invalid format");
        }

        return canonical;
    }

    private static String requireCanonicalText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }

        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " must be canonical");
        }

        return value;
    }

}
