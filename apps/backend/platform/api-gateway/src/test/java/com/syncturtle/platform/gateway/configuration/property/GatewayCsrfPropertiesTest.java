package com.syncturtle.platform.gateway.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GatewayCsrfPropertiesTest {

    @Nested
    class ConstructorTests {

        @Test
        void retainsTheSelectedBoundsAndLifetime() {
            GatewayCsrfProperties properties = new GatewayCsrfProperties(
                    Duration.ofMinutes(30), Duration.ofDays(7), 32, 2048, 1024);
            assertThat(properties.getPreAuthLifetime()).isEqualTo(Duration.ofMinutes(30));
            assertThat(properties.getSessionMaxLifetime()).isEqualTo(Duration.ofDays(7));
            assertThat(properties.getNonceBytes()).isEqualTo(32);
            assertThat(properties.getMaxSignedTokenChars()).isEqualTo(2048);
            assertThat(properties.getMaxPayloadBytes()).isEqualTo(1024);
        }

        @Test
        void rejectsWeakOrIncoherentBounds() {
            assertThatThrownBy(() -> new GatewayCsrfProperties(
                    Duration.ZERO, Duration.ofDays(7), 32, 2048, 1024))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new GatewayCsrfProperties(
                    Duration.ofMinutes(30), Duration.ofDays(7), 31, 2048, 1024))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new GatewayCsrfProperties(
                    Duration.ofMinutes(30), Duration.ofDays(7), 32, 512, 1024))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

}
