package com.syncturtle.services.user.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshSessionSuccessorEnvelopeProperties")
class RefreshSessionSuccessorEnvelopePropertiesTest {

    @Nested
    class ConstructionTests {

        @Test
        @DisplayName("accepts exactly thirty two base 64 url decoded bytes")
        void acceptsExactlyThirtyTwoBase64UrlDecodedBytes() {
            // arrange
            String encodedKey = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
            // conditions
            // act
            RefreshSessionSuccessorEnvelopeProperties result = new RefreshSessionSuccessorEnvelopeProperties(
                    encodedKey);
            // assert
            assertThat(result.getEncryptionKey()).hasSize(32);
            // verify
        }

        @Test
        @DisplayName("rejects missing malformed or wrong length keys")
        void rejectsMissingMalformedOrWrongLengthKeys() {
            // arrange
            String shortKey = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[31]);
            // conditions
            // act + assert
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new RefreshSessionSuccessorEnvelopeProperties(null));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new RefreshSessionSuccessorEnvelopeProperties("not base64url!"));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new RefreshSessionSuccessorEnvelopeProperties(shortKey));
            // verify
        }

        @Test
        @DisplayName("does not expose mutable key material")
        void doesNotExposeMutableKeyMateral() {
            // arrange
            String encodedKey = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
            RefreshSessionSuccessorEnvelopeProperties properties = new RefreshSessionSuccessorEnvelopeProperties(
                    encodedKey);
            // conditions
            // act
            byte[] first = properties.getEncryptionKey();
            first[0] = 1;
            // assert
            assertThat(properties.getEncryptionKey()[0]).isZero();
        }

    }

}
