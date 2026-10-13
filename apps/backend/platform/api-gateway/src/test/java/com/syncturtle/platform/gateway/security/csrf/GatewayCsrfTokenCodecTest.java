package com.syncturtle.platform.gateway.security.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.security.csrf.CsrfTokenSigner;
import com.syncturtle.common.security.csrf.impl.HmacCsrfTokenSigner;
import com.syncturtle.platform.gateway.configuration.property.GatewayCsrfProperties;
import com.syncturtle.platform.gateway.exception.GatewayCsrfTokenException;

import tools.jackson.databind.json.JsonMapper;

class GatewayCsrfTokenCodecTest {

    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private static final String SESSION_ID = "33333333-3333-3333-3333-333333333333";
    private static final String NONCE = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

    private CsrfTokenSigner signer;
    private GatewayCsrfProperties properties;
    private GatewayCsrfTokenProcessor processor;

    @BeforeEach
    void setUp() {
        signer = new HmacCsrfTokenSigner("k".repeat(32).getBytes(StandardCharsets.UTF_8));
        properties = new GatewayCsrfProperties(Duration.ofMinutes(30), Duration.ofDays(7), 32, 2048, 1024);
        processor = processorAt(NOW);
    }

    @Nested
    class PreAuth {

        @Test
        void issuesAndValidatesTheExactCanonicalClaims() {
            IssuedPreAuthCsrfToken issued = processor.issuePreAuth();

            ValidatedPreAuthCsrfToken validated = processor.validatePreAuth(
                    issued.getSignedCookieToken(),
                    issued.getSubmittedToken());

            assertThat(validated.getIssuedAt()).isEqualTo(NOW);
            assertThat(validated.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
            assertThat(decodedJson(issued.getSubmittedToken()))
                    .isEqualTo("{\"v\":1,\"scp\":\"P\",\"iat\":1787832000,\"exp\":1787833800,\"n\":\""
                            + extractedNonce(issued.getSubmittedToken()) + "\"}");
        }

        @Test
        void acceptsImmediatelyBeforeExpiryAndRejectsAtExpiry() {
            IssuedPreAuthCsrfToken issued = processor.issuePreAuth();

            processorAt(issued.getExpiresAt().minusSeconds(1)).validatePreAuth(
                    issued.getSignedCookieToken(),
                    issued.getSubmittedToken());

            assertThatThrownBy(() -> processorAt(issued.getExpiresAt()).validatePreAuth(
                    issued.getSignedCookieToken(),
                    issued.getSubmittedToken()))
                    .isInstanceOf(GatewayCsrfTokenException.class);
        }

        @Test
        void rejectsSessionScope() {
            IssuedSessionCsrfToken issued = processor.issueSession(SESSION_ID, NOW.plus(Duration.ofDays(7)));

            assertThatThrownBy(() -> processor.validatePreAuth(
                    issued.getSignedCookieToken(),
                    issued.getSubmittedToken()))
                    .isInstanceOf(GatewayCsrfTokenException.class);
        }
    }

    @Nested
    class Session {

        @Test
        void bindsCanonicalSessionAndCapsExpiryAtFamilyIdleExpiry() {
            Instant familyIdleExpiry = NOW.plus(Duration.ofHours(12));

            IssuedSessionCsrfToken issued = processor.issueSession(SESSION_ID, familyIdleExpiry);
            ValidatedSessionCsrfToken validated = processor.validateSession(
                    issued.getSignedCookieToken(),
                    issued.getSubmittedToken());

            assertThat(validated.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(validated.getExpiresAt()).isEqualTo(familyIdleExpiry);
            assertThat(decodedJson(issued.getSubmittedToken()))
                    .contains("\"scp\":\"S\"", "\"sid\":\"" + SESSION_ID + "\"");
        }

        @Test
        void capsExpiryAtConfiguredSessionLifetime() {
            IssuedSessionCsrfToken issued = processor.issueSession(SESSION_ID, NOW.plus(Duration.ofDays(20)));

            assertThat(issued.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        }

        @Test
        void rejectsPreAuthScopeInvalidUuidAndInactiveFamily() {
            IssuedPreAuthCsrfToken preAuth = processor.issuePreAuth();

            assertThatThrownBy(() -> processor.validateSession(
                    preAuth.getSignedCookieToken(),
                    preAuth.getSubmittedToken()))
                    .isInstanceOf(GatewayCsrfTokenException.class);
            assertThatThrownBy(() -> processor.issueSession("not-a-uuid", NOW.plusSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> processor.issueSession(SESSION_ID, NOW))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class InvalidToken {

        @Test
        void rejectsMacTamperAndSubmittedPayloadMismatch() {
            IssuedPreAuthCsrfToken issued = processor.issuePreAuth();
            String tampered = issued.getSignedCookieToken().substring(0, issued.getSignedCookieToken().length() - 1)
                    + "A";

            assertInvalidPreAuth(tampered, issued.getSubmittedToken());
            assertInvalidPreAuth(issued.getSignedCookieToken(), issued.getSubmittedToken() + "A");
        }

        @Test
        void rejectsLegacyPaddedAndNonCanonicalBase64UrlValues() {
            assertInvalidPreAuth("legacy-random-token", "legacy-random-token");

            String canonical = preAuthJson(1, "P", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), NONCE);
            String paddedPayload = payload(canonical) + "=";
            assertInvalidPreAuth(signer.sign(paddedPayload), paddedPayload);
            assertInvalidPreAuth(signer.sign("+w"), "+w");
        }

        @Test
        void rejectsReorderedDuplicateUnknownMissingAndIllegalScopeFields() {
            String reordered = "{\"scp\":\"P\",\"v\":1,\"iat\":" + NOW.getEpochSecond()
                    + ",\"exp\":" + NOW.plusSeconds(1800).getEpochSecond() + ",\"n\":\"" + NONCE + "\"}";
            String duplicate = "{\"v\":1,\"v\":1,\"scp\":\"P\",\"iat\":" + NOW.getEpochSecond()
                    + ",\"exp\":" + NOW.plusSeconds(1800).getEpochSecond() + ",\"n\":\"" + NONCE + "\"}";
            String unknown = preAuthJson(1, "P", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), NONCE)
                    .replace("}", ",\"extra\":true}");
            String missing = "{\"v\":1,\"scp\":\"P\",\"iat\":" + NOW.getEpochSecond()
                    + ",\"exp\":" + NOW.plusSeconds(1800).getEpochSecond() + "}";
            String illegalSid = preAuthJson(1, "P", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), NONCE)
                    .replace("}", ",\"sid\":\"" + SESSION_ID + "\"}");

            assertInvalidJson(reordered);
            assertInvalidJson(duplicate);
            assertInvalidJson(unknown);
            assertInvalidJson(missing);
            assertInvalidJson(illegalSid);
        }

        @Test
        void rejectsUnsupportedVersionScopeAndWrongClaimTypes() {
            assertInvalidJson(preAuthJson(2, "P", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), NONCE));
            assertInvalidJson(preAuthJson(1, "X", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), NONCE));
            assertInvalidJson(preAuthJson(1, "P", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), NONCE)
                    .replace("\"v\":1", "\"v\":\"1\""));
            assertInvalidJson(preAuthJson(1, "P", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), NONCE)
                    .replace("\"iat\":" + NOW.getEpochSecond(), "\"iat\":\"" + NOW.getEpochSecond() + "\""));
            assertInvalidJson(preAuthJson(1, "P", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), NONCE)
                    .replace("\"n\":\"" + NONCE + "\"", "\"n\":null"));
        }

        @Test
        void rejectsWrongNonceLengthAndNonCanonicalSessionUuid() {
            String shortNonce = B64URL.encodeToString(new byte[31]);
            assertInvalidJson(preAuthJson(
                    1, "P", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), shortNonce));

            String uppercaseSessionId = "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA";
            String sessionJson = sessionJson(
                    NOW.getEpochSecond(),
                    NOW.plusSeconds(3600).getEpochSecond(),
                    uppercaseSessionId,
                    NONCE);
            assertInvalidSessionJson(sessionJson);
        }

        @Test
        void rejectsFutureIssuedAtInvalidExpiryAndLifetimeExtension() {
            assertInvalidJson(preAuthJson(
                    1, "P", NOW.plusSeconds(1).getEpochSecond(), NOW.plusSeconds(1801).getEpochSecond(), NONCE));
            assertInvalidJson(preAuthJson(1, "P", NOW.getEpochSecond(), NOW.getEpochSecond(), NONCE));
            assertInvalidJson(preAuthJson(
                    1, "P", NOW.getEpochSecond(), NOW.plusSeconds(1801).getEpochSecond(), NONCE));
            assertInvalidSessionJson(sessionJson(
                    NOW.getEpochSecond(),
                    NOW.plus(Duration.ofDays(7)).plusSeconds(1).getEpochSecond(),
                    SESSION_ID,
                    NONCE));
        }

        @Test
        void rejectsConfiguredCharacterAndDecodedPayloadBounds() {
            assertInvalidPreAuth("x".repeat(2049), "x".repeat(2049));

            GatewayCsrfProperties bounded = new GatewayCsrfProperties(
                    Duration.ofMinutes(30), Duration.ofDays(7), 32, 2048, 256);
            GatewayCsrfTokenProcessor boundedProcessor = new GatewayCsrfTokenProcessor(
                    new JsonMapper(),
                    signer,
                    new Base64UrlSecureTokenGenerator(),
                    bounded,
                    Clock.fixed(NOW, ZoneOffset.UTC));
            String oversizedNonce = B64URL.encodeToString(new byte[300]);
            String oversizedJson = preAuthJson(
                    1, "P", NOW.getEpochSecond(), NOW.plusSeconds(1800).getEpochSecond(), oversizedNonce);
            String oversizedPayload = payload(oversizedJson);

            assertThatThrownBy(() -> boundedProcessor.validatePreAuth(
                    signer.sign(oversizedPayload),
                    oversizedPayload))
                    .isInstanceOf(GatewayCsrfTokenException.class);
        }
    }

    private void assertInvalidJson(String json) {
        String submitted = payload(json);
        assertInvalidPreAuth(signer.sign(submitted), submitted);
    }

    private void assertInvalidSessionJson(String json) {
        String submitted = payload(json);
        assertThatThrownBy(() -> processor.validateSession(signer.sign(submitted), submitted))
                .isInstanceOf(GatewayCsrfTokenException.class);
    }

    private void assertInvalidPreAuth(String signed, String submitted) {
        assertThatThrownBy(() -> processor.validatePreAuth(signed, submitted))
                .isInstanceOf(GatewayCsrfTokenException.class);
    }

    private GatewayCsrfTokenProcessor processorAt(Instant instant) {
        return new GatewayCsrfTokenProcessor(
                new JsonMapper(),
                signer,
                new Base64UrlSecureTokenGenerator(),
                properties,
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static String preAuthJson(int version, String scope, long issuedAt, long expiresAt, String nonce) {
        return "{\"v\":" + version + ",\"scp\":\"" + scope + "\",\"iat\":" + issuedAt
                + ",\"exp\":" + expiresAt + ",\"n\":\"" + nonce + "\"}";
    }

    private static String sessionJson(long issuedAt, long expiresAt, String sessionId, String nonce) {
        return "{\"v\":1,\"scp\":\"S\",\"iat\":" + issuedAt + ",\"exp\":" + expiresAt
                + ",\"sid\":\"" + sessionId + "\",\"n\":\"" + nonce + "\"}";
    }

    private static String payload(String json) {
        return B64URL.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodedJson(String submittedToken) {
        return new String(Base64.getUrlDecoder().decode(submittedToken), StandardCharsets.UTF_8);
    }

    private static String extractedNonce(String submittedToken) {
        String json = decodedJson(submittedToken);
        int start = json.indexOf("\"n\":\"") + 5;
        return json.substring(start, json.length() - 2);
    }
}
