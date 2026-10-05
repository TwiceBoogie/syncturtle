package com.syncturtle.common.security.provenance;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.springframework.util.Assert;

import com.syncturtle.common.security.exception.RequestProvenanceException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class RequestProvenanceAssertionSerializer {

    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DECODER = Base64.getUrlDecoder();

    private final JsonMapper jsonMapper;

    public RequestProvenanceAssertionSerializer(JsonMapper jsonMapper) {
        Assert.notNull(jsonMapper, "jsonMapper is required");

        this.jsonMapper = jsonMapper;
    }

    public String encodePayload(RequestProvenanceClaims claims) {
        Assert.notNull(claims, "request provenance claims are required");

        String json = canonicalJson(claims);
        return B64URL.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public String compose(String payload, byte[] signature) {
        Assert.hasText(payload, "payload is required");
        Assert.notNull(signature, "signature is required");
        Assert.isTrue(signature.length == RequestProvenanceProtocol.ED25519_SIGNATURE_BYTES,
                "signature must be an Ed25519 signature");

        String assertion = payload + "." + B64URL.encodeToString(signature);
        if (assertion.length() > RequestProvenanceProtocol.MAX_HEADER_CHARACTERS) {
            throw invalid("generated request provenance assertion is oversized");
        }

        return assertion;
    }

    public DecodedAssertion decode(String assertion) {
        if (assertion == null || assertion.isBlank()
                || assertion.length() > RequestProvenanceProtocol.MAX_HEADER_CHARACTERS) {
            throw invalid("request provenance assertion is missing or oversized");
        }

        if (!assertion.equals(assertion.trim())) {
            throw invalid("request provenance assertion is not canonical");
        }

        int separator = assertion.indexOf('.');
        if (separator <= 0 || separator != assertion.lastIndexOf('.') || separator == assertion.length() - 1) {
            throw invalid("request provenance assertion has invalid segments");
        }

        String payload = assertion.substring(0, separator);
        byte[] jsonBytes = decodeCanonical(payload);
        byte[] signature = decodeCanonical(assertion.substring(separator + 1));

        if (jsonBytes.length > RequestProvenanceProtocol.MAX_PAYLOAD_BYTES
                || signature.length != RequestProvenanceProtocol.ED25519_SIGNATURE_BYTES) {
            throw invalid("request provenance assertion has invalid bounds");
        }

        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        RequestProvenanceClaims claims = parseClaims(json);

        if (!json.equals(canonicalJson(claims))) {
            throw invalid("request provenance payload is not canonical");
        }

        return new DecodedAssertion(claims, payload, signature);
    }

    private RequestProvenanceClaims parseClaims(String json) {
        JsonNode root;
        try {
            root = jsonMapper.readTree(json);
        } catch (JacksonException exception) {
            throw invalid("request provenance payload is malformed", exception);
        }

        if (root == null
                || !root.isObject()
                || root.size() != RequestProvenanceProtocol.EXPECTED_FIELDS.size()) {
            throw invalid("request provenance payload has invalid fields");
        }

        root.properties().forEach(property -> {
            if (!RequestProvenanceProtocol.EXPECTED_FIELDS.contains(property.getKey())) {
                throw invalid("request provenance payload has an unknown field");
            }
        });

        try {
            return RequestProvenanceClaims.decoded(
                    requiredInt(root, "v"),
                    requiredText(root, "alg"),
                    requiredText(root, "kid"),
                    requiredText(root, "iss"),
                    requiredText(root, "aud"),
                    Instant.ofEpochSecond(requiredLong(root, "iat")),
                    Instant.ofEpochSecond(requiredLong(root, "exp")),
                    requiredText(root, "m"),
                    requiredText(root, "p"),
                    requiredText(root, "q"),
                    requiredText(root, "h"));
        } catch (RequestProvenanceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid("request provenance claims are invalid", exception);
        }
    }

    public String canonicalJson(RequestProvenanceClaims claims) {
        return "{\"v\":" + claims.getVersion()
                + ",\"alg\":\"" + claims.getAlgorithm()
                + "\",\"kid\":\"" + claims.getKeyId()
                + "\",\"iss\":\"" + claims.getIssuer()
                + "\",\"aud\":\"" + claims.getAudience()
                + "\",\"iat\":" + claims.getIssuedAt().getEpochSecond()
                + ",\"exp\":" + claims.getExpiresAt().getEpochSecond()
                + ",\"m\":\"" + claims.getMethod()
                + "\",\"p\":\"" + claims.getRawPath()
                + "\",\"q\":\"" + claims.getRawQueryDigest()
                + "\",\"h\":\"" + claims.getProtectedHeaderDigest()
                + "\"}";
    }

    private static byte[] decodeCanonical(String encoded) {
        if (encoded == null || encoded.isBlank() || encoded.indexOf('=') >= 0) {
            throw invalid("request provenance Base64URL value is invalid");
        }

        try {
            byte[] decoded = B64URL_DECODER.decode(encoded);
            if (!B64URL.encodeToString(decoded).equals(encoded)) {
                throw invalid("request provenance Base64URL value is not canonical");
            }

            return decoded;
        } catch (IllegalArgumentException exception) {
            throw invalid("request provenance Base64URL value is malformed", exception);
        }
    }

    private static int requiredInt(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw invalid("request provenance integer claim is invalid");
        }

        return value.intValue();
    }

    private static long requiredLong(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw invalid("request provenance long claim is invalid");
        }

        return value.longValue();
    }

    private static String requiredText(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || !value.isString() || value.stringValue() == null || value.stringValue().isBlank()) {
            throw invalid("request provenance text claim is invalid");
        }

        return value.stringValue();
    }

    private static RequestProvenanceException invalid(String message) {
        return new RequestProvenanceException(message);
    }

    private static RequestProvenanceException invalid(String message, Throwable cause) {
        return new RequestProvenanceException(message, cause);
    }

    public static final class DecodedAssertion {

        private final RequestProvenanceClaims claims;
        private final String payload;
        private final byte[] signature;

        private DecodedAssertion(RequestProvenanceClaims claims, String payload, byte[] signature) {
            this.claims = claims;
            this.payload = payload;
            this.signature = signature.clone();
        }

        public RequestProvenanceClaims getClaims() {
            return claims;
        }

        public String getPayload() {
            return payload;
        }

        public byte[] getSignature() {
            return signature.clone();
        }

    }

}
