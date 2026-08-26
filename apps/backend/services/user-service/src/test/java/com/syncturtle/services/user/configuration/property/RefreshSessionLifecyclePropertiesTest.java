package com.syncturtle.services.user.configuration.property;

import static com.syncturtle.services.user.support.fixture.RefreshSessionPropertyFixtures.lifecycleProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RefreshSessionLifecyclePropertiesTest {

    @Nested
    class Construct {

        @Test
        void acceptsApprovedPhaseTwoDurations() {
            RefreshSessionLifecycleProperties result = lifecycleProperties(
                    Duration.ofDays(7),
                    Duration.ofDays(30),
                    Duration.ofSeconds(5));

            assertThat(result.getIdleLifetime()).isEqualTo(Duration.ofDays(7));
            assertThat(result.getAbsoluteLifetime()).isEqualTo(Duration.ofDays(30));
            assertThat(result.getGraceWindow()).isEqualTo(Duration.ofSeconds(5));
        }

        @Test
        void rejectsDifferentIdleLifetime() {
            assertThatThrownBy(() -> lifecycleProperties(
                    Duration.ofDays(8),
                    Duration.ofDays(30),
                    Duration.ofSeconds(5)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("app.auth.refresh-session.idle-lifetime must be 7 days");
        }

        @Test
        void rejectsDifferentAbsoluteLifetime() {
            assertThatThrownBy(() -> lifecycleProperties(
                    Duration.ofDays(7),
                    Duration.ofDays(31),
                    Duration.ofSeconds(5)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("app.auth.refresh-session.absolute-lifetime must be 30 days");
        }

        @Test
        void rejectsDifferentGraceWindow() {
            assertThatThrownBy(() -> lifecycleProperties(
                    Duration.ofDays(7),
                    Duration.ofDays(30),
                    Duration.ofSeconds(6)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("app.auth.refresh-session.grace-window must be 5 seconds");
        }
    }
}
