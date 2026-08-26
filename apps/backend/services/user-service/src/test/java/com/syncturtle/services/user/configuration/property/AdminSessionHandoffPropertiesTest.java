package com.syncturtle.services.user.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AdminSessionHandoffProperties")
class AdminSessionHandoffPropertiesTest {

    @Nested
    @DisplayName("construction")
    class ConstructionTests {

        @Test
        @DisplayName("accepts the fixed thirty second maximum")
        void acceptsTheFixedThirtySecondMaximum() {
            // arrange
            Duration ttl = Duration.ofSeconds(30);
            // act
            AdminSessionHandoffProperties properties = new AdminSessionHandoffProperties(ttl);
            // assert
            assertThat(properties.getTtl()).isEqualTo(ttl);
        }

        @Test
        @DisplayName("rejects zero, negative, and longer lifetimes")
        void rejectsInvalidLifetimes() {
            // arrange
            Duration zero = Duration.ZERO;
            Duration negative = Duration.ofMillis(-1);
            Duration tooLong = Duration.ofSeconds(30).plusMillis(1);
            // act + assert
            assertThatThrownBy(() -> new AdminSessionHandoffProperties(zero))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new AdminSessionHandoffProperties(negative))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new AdminSessionHandoffProperties(tooLong))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

}
