package com.syncturtle.services.instance.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.instance.support.fixture.InstanceFixtures;

@DisplayName("Instance Entity")
class InstanceTest {

    @Nested
    @DisplayName("register(InstanceRegistrationParam)")
    class Register {

        @Test
        @DisplayName("rejects null registration param")
        void rejectsNullRegistrationParam() {
            assertThatThrownBy(() -> Instance.register(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("registration is required");
        }

        @Test
        @DisplayName("normalizes instanceId and initializes embedded state")
        void normalizesInstanceIdAndInitializesEmbeddedState() {
            Instance instance = Instance.register(InstanceFixtures.registrationParam("Syncturtle"));

            assertThat(instance.getInstanceId()).isEqualTo("instance-001");
            assertThat(instance.getInstanceName()).isEqualTo("Syncturtle");
            assertThat(instance.isTelemetryEnabled()).isTrue();
            assertThat(instance.isSupportRequired()).isFalse();
            assertThat(instance.isTest()).isTrue();
            assertThat(instance.isSetupDone()).isFalse();
            assertThat(instance.getRuntime().getNamespace()).isEqualTo("dev");
            assertThat(instance.getUpdateCheck().getCurrentVersion()).isEqualTo("0.0.1-test");
            assertThat(instance.getConfig().getVersion()).isZero();
        }

    }

    @Nested
    @DisplayName("rename(String)")
    class Rename {

        @Test
        @DisplayName("trims and stores new name")
        void trimsAndStoresNewName() {
            Instance instance = InstanceFixtures.activeInstance("Old Name");
            instance.rename("  New Name  ");
            assertThat(instance.getInstanceName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("rejects blank names")
        void rejectsBlankNames() {
            Instance instance = InstanceFixtures.activeInstance("Old Name");
            assertThatThrownBy(() -> instance.rename("    "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("instanceName is required");
        }

        @Test
        @DisplayName("rejects mutation when aggregate is soft-deleted")
        void rejectsMutationWhenAggregateIsSoftDeleted() {
            Instance instance = InstanceFixtures.deletedInstance("Old Name");
            assertThatThrownBy(() -> instance.rename("New Name"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Instance is deleted");
        }

    }

    @Nested
    @DisplayName("markSignupScreenVisited()")
    class MarkSignupScreenVisited {

        @Test
        @DisplayName("is idempotent")
        void isIdempotent() {
            Instance instance = InstanceFixtures.activeInstance("Syncturtle");
            instance.markSignupScreenVisited();
            instance.markSignupScreenVisited();
            assertThat(instance.isSignupScreenVisited()).isTrue();
        }

    }

    @Nested
    @DisplayName("bumpConfigVersion(Clock)")
    class BumpConfigVersion {

        @Test
        @DisplayName("increments initialized config version")
        void incrementsInitializedConfigVersion() {
            Instance instance = InstanceFixtures.activeInstance("Syncturtle");
            Clock clock = Clock.fixed(Instant.parse("2026-05-23T15:00:00Z"), ZoneOffset.UTC);
            instance.bumpConfigVersion(clock);
            assertThat(instance.getConfig().getVersion()).isEqualTo(1L);
            assertThat(instance.getConfig().getLastCheckedAt()).isEqualTo(Instant.parse("2026-05-23T15:00:00Z"));
        }

        @Test
        @DisplayName("rejects null clock")
        void rejectsNullClock() {
            Instance instance = InstanceFixtures.activeInstance("Syncturtle");
            assertThatThrownBy(() -> instance.bumpConfigVersion(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("clock is required");
        }

    }

}
