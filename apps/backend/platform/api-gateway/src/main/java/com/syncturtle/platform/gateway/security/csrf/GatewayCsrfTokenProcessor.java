package com.syncturtle.platform.gateway.security.csrf;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.security.csrf.CsrfTokenSigner;
import com.syncturtle.platform.gateway.configuration.property.GatewayCsrfProperties;
import com.syncturtle.platform.gateway.exception.GatewayCsrfTokenException;
import com.syncturtle.platform.gateway.type.GatewayCsrfScope;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class GatewayCsrfTokenProcessor {

    private static final int CURRENT_VERSION = 1;
    private static final Set<String> PREAUTH_FIELDS = Set.of("v", "scp", "iat", "exp", "n");
    private static final Set<String> SESSION_FIELDS = Set.of("v", "scp", "iat", "exp", "sid", "n");
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DECODER = Base64.getUrlDecoder();

    private final JsonMapper jsonMapper;
    private final CsrfTokenSigner tokenSigner;
    private final SecureTokenGenerator tokenGenerator;
    private final GatewayCsrfProperties properties;
    private final Clock clock;

    public GatewayCsrfTokenProcessor(
            JsonMapper jsonMapper,
            CsrfTokenSigner tokenSigner,
            SecureTokenGenerator tokenGenerator,
            GatewayCsrfProperties properties,
            Clock clock) {
        this.jsonMapper = jsonMapper;
        this.tokenSigner = tokenSigner;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
        this.properties = properties;
    }

    public IssuedPreAuthCsrfToken issuePreAuth() {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(properties.getPreAuthLifetime());
        String nonce = tokenGenerator.generateBase64Url(properties.getNonceBytes());
        String json = canonicalPreAuthJson(issuedAt, expiresAt, nonce);
        String payload = encodePayload(json);

        return new IssuedPreAuthCsrfToken(payload, tokenSigner.sign(payload), issuedAt, expiresAt);
    }

    public IssuedSessionCsrfToken issueSession(String sessionId, Instant validatedFamilyIdleExpiresAt) {
        String canonicalSessionId = canonicalUuid(sessionId);
        if (validatedFamilyIdleExpiresAt == null) {
            throw new IllegalArgumentException("validatedFamilyIdleExpiresAt is required");
        }

        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(properties.getSessionMaxLifetime());
        if (validatedFamilyIdleExpiresAt.isBefore(expiresAt)) {
            expiresAt = validatedFamilyIdleExpiresAt;
        }

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("validated family must remain active after issuance");
        }

        String nonce = tokenGenerator.generateBase64Url(properties.getNonceBytes());
        String json = canonicalSessionJson(issuedAt, expiresAt, canonicalSessionId, nonce);
        String payload = encodePayload(json);

        return new IssuedSessionCsrfToken(
                payload,
                tokenSigner.sign(payload),
                canonicalSessionId,
                issuedAt,
                expiresAt);
    }

    public ValidatedPreAuthCsrfToken validatePreAuth(String signedCookieToken, String submittedToken) {
        ParsedClaims claims = parseAndValidate(signedCookieToken, submittedToken, GatewayCsrfScope.PREAUTH);
        String canonical = canonicalPreAuthJson(claims.issuedAt, claims.expiresAt, claims.nonce);
        requireCanonicalPayload(claims.payload, canonical);
        requireExactPreAuthLifetime(claims);

        return new ValidatedPreAuthCsrfToken(
                claims.payload,
                signedCookieToken,
                claims.issuedAt,
                claims.expiresAt);
    }

    public ValidatedSessionCsrfToken validateSession(String signedCookieToken, String submittedToken) {
        ParsedClaims claims = parseAndValidate(signedCookieToken, submittedToken, GatewayCsrfScope.SESSION);
        String canonicalSessionId = canonicalUuidOrFailure(claims.sessionId);
        String canonical = canonicalSessionJson(
                claims.issuedAt,
                claims.expiresAt,
                canonicalSessionId,
                claims.nonce);
        requireCanonicalPayload(claims.payload, canonical);

        if (claims.expiresAt.isAfter(claims.issuedAt.plus(properties.getSessionMaxLifetime()))) {
            throw invalid();
        }

        return new ValidatedSessionCsrfToken(
                claims.payload,
                signedCookieToken,
                canonicalSessionId,
                claims.issuedAt,
                claims.expiresAt);
    }

    private ParsedClaims parseAndValidate(
            String signedCookieToken,
            String submitedToken,
            GatewayCsrfScope requiredScope) {
        requireBoundedText(signedCookieToken, properties.getMaxSignedTokenChars());
        requireBoundedText(submitedToken, properties.getMaxSignedTokenChars());

        if (!tokenSigner.verify(signedCookieToken)) {
            throw invalid();
        }

        String payload = tokenSigner.extractToken(signedCookieToken);
        if (!constantTimeEquals(payload, submitedToken)) {
            throw invalid();
        }

        byte[] jsonBytes = decodeCanonicalBase64Url(payload);
        if (jsonBytes.length > properties.getMaxPayloadBytes()) {
            throw invalid();
        }

        JsonNode root;
        try {
            root = jsonMapper.readTree(new String(jsonBytes, StandardCharsets.UTF_8));
        } catch (JacksonException exception) {
            throw invalid();
        }

        if (root == null || !root.isObject()) {
            throw invalid();
        }

        Set<String> expectedFields = requiredScope == GatewayCsrfScope.PREAUTH
                ? PREAUTH_FIELDS
                : SESSION_FIELDS;
        if (root.size() != expectedFields.size()) {
            throw invalid();
        }

        root.properties().forEach(property -> {
            if (!expectedFields.contains(property.getKey())) {
                throw invalid();
            }
        });

        int version = requiredInt(root, "v");
        String scope = requiredText(root, "scp");
        long issuedAtEpochSecond = requiredLong(root, "iat");
        long expiresAtEpochSecond = requiredLong(root, "exp");
        String nonce = requiredText(root, "n");
        String sessionId = requiredScope == GatewayCsrfScope.SESSION ? requiredText(root, "sid") : null;

        if (version != CURRENT_VERSION || !requiredScope.getClaimValue().equals(scope)) {
            throw invalid();
        }

        validateNonce(nonce);

        Instant issuedAt;
        Instant expiresAt;
        try {
            issuedAt = Instant.ofEpochSecond(issuedAtEpochSecond);
            expiresAt = Instant.ofEpochSecond(expiresAtEpochSecond);
        } catch (RuntimeException exception) {
            throw invalid();
        }

        Instant now = Instant.now(clock);
        if (issuedAt.isAfter(now) || !expiresAt.isAfter(now) || !expiresAt.isAfter(issuedAt)) {
            throw invalid();
        }

        return new ParsedClaims(payload, issuedAt, expiresAt, sessionId, nonce);
    }

    private void requireExactPreAuthLifetime(ParsedClaims claims) {
        if (!claims.issuedAt.plus(properties.getPreAuthLifetime()).equals(claims.expiresAt)) {
            throw invalid();
        }
    }

    private static String canonicalPreAuthJson(Instant issuedAt, Instant expiresAt, String nonce) {
        return "{\"v\":1,\"scp\":\"P\",\"iat\":" + issuedAt.getEpochSecond()
                + ",\"exp\":" + expiresAt.getEpochSecond()
                + ",\"n\":\"" + nonce + "\"}";
    }

    private static String canonicalSessionJson(
            Instant issuedAt,
            Instant expiresAt,
            String sessionId,
            String nonce) {
        return "{\"v\":1,\"scp\":\"S\",\"iat\":" + issuedAt.getEpochSecond()
                + ",\"exp\":" + expiresAt.getEpochSecond()
                + ",\"sid\":\"" + sessionId
                + "\",\"n\":\"" + nonce + "\"}";
    }

    private String encodePayload(String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > properties.getMaxPayloadBytes()) {
            throw new IllegalStateException("Generated CSRF payload exceeds configured bound");
        }

        return B64URL.encodeToString(bytes);
    }

    private static byte[] decodeCanonicalBase64Url(String payload) {
        if (payload == null || payload.isBlank() || payload.indexOf('=') >= 0) {
            throw invalid();
        }

        try {
            byte[] decoded = B64URL_DECODER.decode(payload);
            if (!B64URL.encodeToString(decoded).equals(payload)) {
                throw invalid();
            }

            return decoded;
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private void validateNonce(String nonce) {
        byte[] decoded = decodeCanonicalBase64Url(nonce);
        if (decoded.length != properties.getNonceBytes()) {
            throw invalid();
        }
    }

    private static void requireCanonicalPayload(String payload, String canonicalJson) {
        String canonicalPayload = B64URL.encodeToString(canonicalJson.getBytes(StandardCharsets.UTF_8));
        if (!constantTimeEquals(canonicalPayload, payload)) {
            throw invalid();
        }
    }

    private static int requiredInt(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || !value.isInt()) {
            throw invalid();
        }

        return value.intValue();
    }

    private static long requiredLong(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw invalid();
        }

        return value.longValue();
    }

    private static String requiredText(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw invalid();
        }

        return value.stringValue();
    }

    private static String canonicalUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        String candidate = value.trim();
        UUID parsed = UUID.fromString(candidate);

        if (!parsed.toString().equals(candidate)) {
            throw new IllegalArgumentException("sessionId must use canonical UUID representation");
        }

        return candidate;
    }

    private static String canonicalUuidOrFailure(String value) {
        try {
            return canonicalUuid(value);
        } catch (RuntimeException exception) {
            throw invalid();
        }
    }

    private static void requireBoundedText(String value, int maximumCharacters) {
        if (value == null || value.isBlank() || value.length() > maximumCharacters) {
            throw invalid();
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }

        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    private static GatewayCsrfTokenException invalid() {
        return new GatewayCsrfTokenException();
    }

    private static final class ParsedClaims {
        private final String payload;
        private final Instant issuedAt;
        private final Instant expiresAt;
        private final String sessionId;
        private final String nonce;

        private ParsedClaims(
                String payload,
                Instant issuedAt,
                Instant expiresAt,
                String sessionId,
                String nonce) {
            this.payload = payload;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.sessionId = sessionId;
            this.nonce = nonce;
        }
    }

}
