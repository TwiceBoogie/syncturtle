package com.syncturtle.common.security.provenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.exception.RequestProvenanceException;
import com.syncturtle.common.security.property.RequestProvenanceKeyProperties;

import tools.jackson.databind.json.JsonMapper;

class RequestProvenanceSignerVerifierTest {

    private static final Instant NOW = Instant.parse("2026-09-03T12:00:00Z");
    private static final RequestHeaderSource NO_HEADERS = name -> List.of();

    private RequestProvenanceAssertionSerializer serializer;
    private RequestProvenanceCanonicalizer canonicalizer;
    private RequestProvenanceKeyLoader keyLoader;
    private KeyPair keyPair;

    @BeforeEach
    void setup() throws Exception {
        serializer = new RequestProvenanceAssertionSerializer(JsonMapper.builder().build());
        canonicalizer = new RequestProvenanceCanonicalizer();
        keyLoader = new RequestProvenanceKeyLoader();
        keyPair = KeyPairGenerator.getInstance(RequestProvenanceProtocol.ALGORITHM).generateKeyPair();
    }

    @Nested
    class SignAndVerifyTests {

        @Test
        void acceptsValidAssertionAndBindsTheCompleteRequestShape() {
            // arrange
            RequestProvenanceSigner signer = signer(keyPair, NOW);
            RequestProvenanceVerifier verifier = verifier(keyPair, NOW);
            RequestHeaderSource headers = singleHeader(GatewayHeaders.HDR_AUTH_USER_ID, "user-1");
            // conditions
            // act
            String assertion = signer.sign("user-service", "post", "/auth/refresh", "a=1&b=2", headers);
            RequestProvenanceClaims claims = verifier.verify(assertion, "POST", "/auth/refresh", "a=1&b=2", headers);
            // assert
            assertThat(claims.getIssuer()).isEqualTo("api-gateway");
            assertThat(claims.getAudience()).isEqualTo("user-service");
            assertThat(claims.getExpiresAt()).isEqualTo(NOW.plusSeconds(10));
            // verify
        }

        @Test
        void rejectsWrongMethodPathQueryAndProtectedHeaders() {
            // arrange
            RequestProvenanceSigner signer = signer(keyPair, NOW);
            RequestProvenanceVerifier verifier = verifier(keyPair, NOW);
            RequestHeaderSource headers = singleHeader(GatewayHeaders.HDR_AUTH_USER_ID, "user-1");
            // conditions
            // act
            String assertion = signer.sign("user-service", "POST", "/auth/refresh", "a=1", headers);
            // assert
            assertThatThrownBy(() -> verifier.verify(assertion, "GET", "/auth/refresh", "a=1", headers))
                    .isInstanceOf(RequestProvenanceException.class);
            assertThatThrownBy(() -> verifier.verify(assertion, "POST", "/auth/sign-out", "a=1", headers))
                    .isInstanceOf(RequestProvenanceException.class);
            assertThatThrownBy(() -> verifier.verify(assertion, "POST", "/auth/refresh", "a=2", headers))
                    .isInstanceOf(RequestProvenanceException.class);
            assertThatThrownBy(() -> verifier.verify(assertion, "POST", "/auth/refresh", "a=1", NO_HEADERS))
                    .isInstanceOf(RequestProvenanceException.class);
            assertThatThrownBy(() -> verifier(keyPair, NOW, "instance-service").verify(
                    assertion, "POST", "/auth/refresh", "a=1", headers))
                    .isInstanceOf(RequestProvenanceException.class);
            // verify
        }

        @Test
        void rejectsUnknownKeyBadSignatureMalformedEncodingAndMalformedClaims() throws Exception {
            String assertion = signer(keyPair, NOW).sign(
                    "user-service", "POST", "/auth/refresh", null, NO_HEADERS);
            KeyPair unknownPair = KeyPairGenerator.getInstance(RequestProvenanceProtocol.ALGORITHM)
                    .generateKeyPair();
            assertThatThrownBy(() -> verifier(unknownPair, NOW).verify(
                    assertion, "POST", "/auth/refresh", null, NO_HEADERS))
                    .isInstanceOf(RequestProvenanceException.class);

            int separator = assertion.indexOf('.');
            byte[] signature = Base64.getUrlDecoder().decode(assertion.substring(separator + 1));
            signature[0] ^= 1;
            String badSignature = assertion.substring(0, separator + 1)
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
            assertThatThrownBy(() -> verifier(keyPair, NOW).verify(
                    badSignature, "POST", "/auth/refresh", null, NO_HEADERS))
                    .isInstanceOf(RequestProvenanceException.class);
            assertThatThrownBy(() -> verifier(keyPair, NOW).verify(
                    "%%%...", "POST", "/auth/refresh", null, NO_HEADERS))
                    .isInstanceOf(RequestProvenanceException.class);

            String malformedJson = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    "{\"v\":2}".getBytes(StandardCharsets.UTF_8));
            String malformed = malformedJson + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(new byte[64]);
            assertThatThrownBy(() -> verifier(keyPair, NOW).verify(
                    malformed, "POST", "/auth/refresh", null, NO_HEADERS))
                    .isInstanceOf(RequestProvenanceException.class);
        }

        @Test
        void rejectsExpiredAndTooFarFutureButAllowsFiveSecondSkew() {
            String assertion = signer(keyPair, NOW).sign(
                    "user-service", "POST", "/auth/refresh", null, NO_HEADERS);
            verifier(keyPair, NOW.plusSeconds(15)).verify(
                    assertion, "POST", "/auth/refresh", null, NO_HEADERS);
            verifier(keyPair, NOW.minusSeconds(5)).verify(
                    assertion, "POST", "/auth/refresh", null, NO_HEADERS);
            assertThatThrownBy(() -> verifier(keyPair, NOW.plusSeconds(16)).verify(
                    assertion, "POST", "/auth/refresh", null, NO_HEADERS))
                    .isInstanceOf(RequestProvenanceException.class);
            assertThatThrownBy(() -> verifier(keyPair, NOW.minusSeconds(6)).verify(
                    assertion, "POST", "/auth/refresh", null, NO_HEADERS))
                    .isInstanceOf(RequestProvenanceException.class);
        }

        @Test
        void acceptsCurrentAndNextVerificationKeysForOneIssuer() throws Exception {
            KeyPair nextPair = KeyPairGenerator.getInstance(RequestProvenanceProtocol.ALGORITHM)
                    .generateKeyPair();
            RequestProvenancePolicy policy = RequestProvenancePolicy.userService();
            Map<String, List<org.springframework.core.io.Resource>> verification = verificationLocations(
                    policy, keyPair);
            verification.put("api-gateway", List.of(
                    new ByteArrayResource(keyPair.getPublic().getEncoded()),
                    new ByteArrayResource(nextPair.getPublic().getEncoded())));
            RequestProvenanceKeyProperties keyProperties = new RequestProvenanceKeyProperties(
                    signerMaterial(keyPair),
                    verification);
            RequestProvenanceVerificationKeys keys = new RequestProvenanceVerificationKeys(policy, keyProperties,
                    keyLoader);
            RequestProvenanceVerifier verifier = new RequestProvenanceVerifier(
                    serializer,
                    canonicalizer,
                    keys,
                    policy,
                    Clock.fixed(NOW, ZoneOffset.UTC));
            String assertion = signer(nextPair, NOW).sign(
                    "user-service", "POST", "/auth/refresh", null, NO_HEADERS);

            RequestProvenanceClaims claims = verifier.verify(
                    assertion, "POST", "/auth/refresh", null, NO_HEADERS);

            assertThat(claims.getKeyId()).isEqualTo(keyLoader.keyId(nextPair.getPublic()));
        }

    }

    @Nested
    class Canonicalization {

        @Test
        void isDeterministicAndRejectsDuplicateProtectedHeaders() {
            RequestHeaderSource first = singleHeader(GatewayHeaders.HDR_AUTH_USER_ID, "user-1");
            RequestHeaderSource second = name -> name.equalsIgnoreCase(GatewayHeaders.HDR_AUTH_USER_ID)
                    ? List.of("user-1")
                    : List.of();
            assertThat(canonicalizer.digestProtectedHeaders(first))
                    .isEqualTo(canonicalizer.digestProtectedHeaders(second));
            assertThat(canonicalizer.digestRawQuery("b=2&a=1"))
                    .isNotEqualTo(canonicalizer.digestRawQuery("a=1&b=2"));

            RequestHeaderSource duplicate = name -> name.equalsIgnoreCase(GatewayHeaders.HDR_AUTH_USER_ID)
                    ? List.of("user-1", "user-2")
                    : List.of();
            assertThatThrownBy(() -> canonicalizer.digestProtectedHeaders(duplicate))
                    .isInstanceOf(RequestProvenanceException.class);
        }

        @Test
        void rejectsVersionAndAlgorithmMismatchBeforeSignatureVerification() {
            assertThatThrownBy(() -> RequestProvenanceClaims.decoded(
                    2, RequestProvenanceProtocol.ALGORITHM, "A".repeat(22), "api-gateway", "user-service",
                    NOW, NOW.plusSeconds(10), "GET", "/api/users", "A".repeat(43), "B".repeat(43)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> RequestProvenanceClaims.decoded(
                    1, "HS256", "A".repeat(22), "api-gateway", "user-service",
                    NOW, NOW.plusSeconds(10), "GET", "/api/users", "A".repeat(43), "B".repeat(43)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private RequestProvenanceSigner signer(KeyPair pair, Instant now) {
        RequestProvenanceSigningKey signingKey = keyLoader.loadSigningKey(
                new ByteArrayResource(pair.getPrivate().getEncoded()),
                new ByteArrayResource(pair.getPublic().getEncoded()));

        return new RequestProvenanceSigner(
                serializer,
                canonicalizer,
                signingKey,
                RequestProvenancePolicy.apiGateway(),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private RequestProvenanceVerifier verifier(KeyPair pair, Instant now) {
        return verifier(pair, now, "user-service");
    }

    private RequestProvenanceVerifier verifier(KeyPair pair, Instant now, String audience) {
        RequestProvenancePolicy policy = switch (audience) {
            case "user-service" -> RequestProvenancePolicy.userService();
            case "instance-service" -> RequestProvenancePolicy.instanceService();
            default -> throw new IllegalArgumentException("unsupported test audience");
        };

        RequestProvenanceKeyProperties keyProperties = keyProperties(policy, pair);
        RequestProvenanceVerificationKeys keys = new RequestProvenanceVerificationKeys(
                policy,
                keyProperties,
                keyLoader);

        return new RequestProvenanceVerifier(serializer, canonicalizer, keys, policy, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static RequestProvenanceKeyProperties keyProperties(RequestProvenancePolicy policy, KeyPair pair) {
        return new RequestProvenanceKeyProperties(signerMaterial(pair), verificationLocations(policy, pair));
    }

    private static RequestProvenanceKeyProperties.SignerMaterial signerMaterial(KeyPair pair) {
        return new RequestProvenanceKeyProperties.SignerMaterial(
                new ByteArrayResource(pair.getPrivate().getEncoded()),
                new ByteArrayResource(pair.getPublic().getEncoded()));
    }

    private static Map<String, List<Resource>> verificationLocations(RequestProvenancePolicy policy, KeyPair pair) {
        Map<String, List<Resource>> verifications = new LinkedHashMap<>();
        for (String issuer : policy.getTrustedIssuerIds()) {
            verifications.put(issuer, List.of(new ByteArrayResource(pair.getPublic().getEncoded())));
        }

        return verifications;
    }

    private static RequestHeaderSource singleHeader(String headerName, String value) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put(headerName.toLowerCase(), List.of(value));

        return name -> values.getOrDefault(name.toLowerCase(), List.of());
    }

}
