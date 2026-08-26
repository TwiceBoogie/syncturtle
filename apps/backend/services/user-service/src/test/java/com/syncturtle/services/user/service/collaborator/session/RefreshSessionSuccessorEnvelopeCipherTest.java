package com.syncturtle.services.user.service.collaborator.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.user.configuration.property.RefreshSessionSuccessorEnvelopeProperties;
import com.syncturtle.services.user.exception.RefreshSessionSuccessorEnvelopeException;

@DisplayName("RefreshSessionSuccessorEnvelopeCipher")
class RefreshSessionSuccessorEnvelopeCipherTest {

    private static final String SID = "11111111-1111-1111-1111-111111111111";
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-13T12:00:05Z");
    private static final String TOKEN = SID + ".raw-secret-for-test";

    private RefreshSessionSuccessorEnvelopeCipher cipher;

    @BeforeEach
    void setup() {
        byte[] ephemeralTestKey = new byte[32];
        for (int i = 0; i < ephemeralTestKey.length; i++) {
            ephemeralTestKey[i] = (byte) i;
        }
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(ephemeralTestKey);
        RefreshSessionSuccessorEnvelopeProperties properties = new RefreshSessionSuccessorEnvelopeProperties(encoded);
        cipher = new RefreshSessionSuccessorEnvelopeCipher(properties, new SecureRandom());
    }

    @Nested
    @DisplayName("Encrypt and Decrypt")
    class EncryptAndDecryptTests {

        @Test
        @DisplayName("round trips without persisting plaintext")
        void roundTripsWithoutPersistingPlaintext() {
            // arrange
            long counter = 1L;
            // conditions
            // act
            String envelope = cipher.encrypt(TOKEN, SID, counter, EXPIRES_AT);
            String result = cipher.decrypt(envelope, SID, counter, EXPIRES_AT);
            // assert
            assertThat(result).isEqualTo(TOKEN);
            assertThat(envelope).startsWith("v1.").doesNotContain(TOKEN).doesNotContain("raw-secret");
            // verify
        }

        @Test
        @DisplayName("uses a unique nonce for each envelope")
        void usesAUniqueNonceForEachEnvelope() {
            // arrange
            long counter = 1L;
            // conditions
            // act
            String first = cipher.encrypt(TOKEN, SID, counter, EXPIRES_AT);
            String second = cipher.encrypt(TOKEN, SID, counter, EXPIRES_AT);
            // assert
            assertThat(first).isNotEqualTo(second);
            // verify
        }

        @Test
        @DisplayName("fails closed for tampering or associated data mismatch")
        void failsClosedForTamperingOrAssociatedDataMismatch() {
            // arrange
            String envelope = cipher.encrypt(TOKEN, SID, 1L, EXPIRES_AT);
            char last = envelope.charAt(envelope.length() - 1);
            char replacement = last == 'A' ? 'B' : 'A';
            String tampered = envelope.substring(0, envelope.length() - 1) + replacement;
            // conditions
            // act + assert
            assertThatThrownBy(() -> cipher.decrypt(tampered, SID, 1L, EXPIRES_AT))
                    .isInstanceOf(RefreshSessionSuccessorEnvelopeException.class);
            assertThatThrownBy(() -> cipher.decrypt(envelope, SID, 2L, EXPIRES_AT))
                    .isInstanceOf(RefreshSessionSuccessorEnvelopeException.class);
            assertThatThrownBy(() -> cipher.decrypt(envelope, SID, 1L, EXPIRES_AT.plusSeconds(1)))
                    .isInstanceOf(RefreshSessionSuccessorEnvelopeException.class);
            // verify
        }

    }

}
