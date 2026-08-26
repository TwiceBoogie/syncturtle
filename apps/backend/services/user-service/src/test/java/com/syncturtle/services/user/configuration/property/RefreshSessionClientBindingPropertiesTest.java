package com.syncturtle.services.user.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RefreshSessionClientBindingPropertiesTest {

    @Nested
    class Construction {

        @Test
        void acceptsExactlyThirtyTwoDisposableBytesAndReturnsDefensiveCopies() {
            // arrange
            byte[] key = new byte[RefreshSessionClientBindingProperties.KEY_BYTES];
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(key);

            // act
            RefreshSessionClientBindingProperties properties = new RefreshSessionClientBindingProperties(encoded);
            byte[] first = properties.getHmacKey();
            first[0] = 1;

            // assert
            assertThat(properties.getHmacKey()).containsOnly((byte) 0);
        }

        @Test
        void rejectsMissingMalformedAndWrongLengthKeys() {
            // arrange, act and assert
            assertThatThrownBy(() -> new RefreshSessionClientBindingProperties(" "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new RefreshSessionClientBindingProperties("not-base64!"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new RefreshSessionClientBindingProperties("AQID"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
