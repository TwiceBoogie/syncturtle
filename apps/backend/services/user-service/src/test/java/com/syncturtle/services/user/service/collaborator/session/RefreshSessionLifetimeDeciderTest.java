package com.syncturtle.services.user.service.collaborator.session;

import static com.syncturtle.services.user.support.fixture.RefreshSessionPropertyFixtures.lifecycleProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.services.user.type.RefreshSessionLifetimeStatus;

@DisplayName("RefreshSessionLifetimeDecider")
class RefreshSessionLifetimeDeciderTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-10T12:00:00Z");

    @Nested
    @DisplayName("RefreshSessionLifetimeDecision.builder()")
    class LifetimeDecisionConstructionTests {

        @Test
        @DisplayName("rejects effective time before stored last used")
        void rejectsEffectiveTimeBeforeStoredLastUse() {
            // arrange
            Instant lastUsedAt = CREATED_AT.plus(Duration.ofDays(2));
            // conditions
            // act + assert
            assertThatThrownBy(() -> RefreshSessionLifetimeDecision.builder()
                    .status(RefreshSessionLifetimeStatus.ROTATABLE)
                    .effectiveNow(lastUsedAt.minusNanos(1))
                    .createdAt(CREATED_AT)
                    .lastUsedAt(lastUsedAt)
                    .idleExpiresAt(lastUsedAt.plus(Duration.ofDays(7)))
                    .absoluteExpiresAt(CREATED_AT.plus(Duration.ofDays(30)))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("effectiveNow must not be before lastUsedAt");
            // verify
        }

    }

    @Nested
    @DisplayName("decideForCreation()")
    class DecideForCreationTests {

        @Test
        @DisplayName("creates seven day idle and thirty day absolute deadlines")
        void createsSevenDayIdleAndThirtyDayAbsoluteDeadlines() {
            // arrange
            RefreshSessionLifetimeDecider policy = policyAt(CREATED_AT);
            // conditions
            // act
            RefreshSessionLifetimeDecision result = policy.decideForCreation();
            // assert
            assertThat(result.getStatus()).isEqualTo(RefreshSessionLifetimeStatus.CREATED);
            assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(result.getLastUsedAt()).isEqualTo(CREATED_AT);
            assertThat(result.getIdleExpiresAt()).isEqualTo(CREATED_AT.plus(Duration.ofDays(7)));
            assertThat(result.getAbsoluteExpiresAt()).isEqualTo(CREATED_AT.plus(Duration.ofDays(30)));
            // verify
        }

    }

    @Nested
    @DisplayName("decideForRotation(RefreshSessionFamilyRecord)")
    class DecideForRotationTests {

        @Test
        @DisplayName("slides idle deadline while preserving creation and absolute deadline")
        void slidesIdleDeadlineWhilePreservingCreationAndAbsoluteDeadline() {
            // arrange
            Instant currentLastUse = CREATED_AT.plus(Duration.ofDays(2));
            RefreshSessionFamilyRecord record = recordAt(currentLastUse, 2L);
            Instant clockNow = CREATED_AT.plus(Duration.ofDays(3));
            RefreshSessionLifetimeDecider policy = policyAt(clockNow);
            // conditions
            // act
            RefreshSessionLifetimeDecision result = policy.decideForRotation(record);
            // assert
            assertThat(result.getStatus()).isEqualTo(RefreshSessionLifetimeStatus.ROTATABLE);
            assertThat(result.getEffectiveNow()).isEqualTo(clockNow);
            assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(result.getLastUsedAt()).isEqualTo(clockNow);
            assertThat(result.getIdleExpiresAt()).isEqualTo(clockNow.plus(Duration.ofDays(7)));
            assertThat(result.getAbsoluteExpiresAt()).isEqualTo(CREATED_AT.plus(Duration.ofDays(30)));
            // verify
        }

        @Test
        @DisplayName("caps idle deadline at absolute deadline")
        void capsIdleDeadlineAtAbsoluteDeadline() {
            // arrange
            Instant currentLastUse = CREATED_AT.plus(Duration.ofDays(24));
            RefreshSessionFamilyRecord record = recordAt(currentLastUse, 8L);
            Instant clockNow = CREATED_AT.plus(Duration.ofDays(25));
            RefreshSessionLifetimeDecider policy = policyAt(clockNow);
            // conditions
            // act
            RefreshSessionLifetimeDecision result = policy.decideForRotation(record);
            // assert
            assertThat(result.getStatus()).isEqualTo(RefreshSessionLifetimeStatus.ROTATABLE);
            assertThat(result.getIdleExpiresAt()).isEqualTo(CREATED_AT.plus(Duration.ofDays(30)));
            assertThat(result.getAbsoluteExpiresAt()).isEqualTo(CREATED_AT.plus(Duration.ofDays(30)));
            // verify
        }

        @Test
        @DisplayName("uses stored last use when clock moves backward")
        void usesStoredLastUseWhenClockMovesBackward() {
            // arrange
            Instant storedLastUse = CREATED_AT.plus(Duration.ofDays(3));
            RefreshSessionFamilyRecord record = recordAt(storedLastUse, 3L);
            RefreshSessionLifetimeDecider policy = policyAt(CREATED_AT.plus(Duration.ofDays(2)));
            // conditions
            // act
            RefreshSessionLifetimeDecision result = policy.decideForRotation(record);
            // assert
            assertThat(result.getStatus()).isEqualTo(RefreshSessionLifetimeStatus.ROTATABLE);
            assertThat(result.getEffectiveNow()).isEqualTo(storedLastUse);
            assertThat(result.getLastUsedAt()).isEqualTo(storedLastUse);
            assertThat(result.getIdleExpiresAt()).isEqualTo(storedLastUse.plus(Duration.ofDays(7)));
            // verify
        }

        @Test
        @DisplayName("treats exact idel deadline as expired")
        void treatsExactIdleDeadlineAsExpired() {
            // arrange
            Instant lastUsedAt = CREATED_AT.plus(Duration.ofDays(2));
            RefreshSessionFamilyRecord record = recordAt(lastUsedAt, 2L);
            RefreshSessionLifetimeDecider policy = policyAt(lastUsedAt.plus(Duration.ofDays(7)));
            // conditions
            // act
            RefreshSessionLifetimeDecision result = policy.decideForRotation(record);
            // assert
            assertThat(result.getStatus()).isEqualTo(RefreshSessionLifetimeStatus.IDLE_EXPIRED);
            assertThat(result.getLastUsedAt()).isEqualTo(lastUsedAt);
            assertThat(result.getIdleExpiresAt()).isEqualTo(lastUsedAt.plus(Duration.ofDays(7)));
            // verify
        }

        @Test
        @DisplayName("treats exact absolute deadline as expired before idle classification")
        void treatsExactAbsoluteDeadlineAsExpiredBeforeIdleClassification() {
            // arrange
            Instant lastUsedAt = CREATED_AT.plus(Duration.ofDays(24));
            RefreshSessionFamilyRecord record = recordAt(lastUsedAt, 8L);
            RefreshSessionLifetimeDecider policy = policyAt(CREATED_AT.plus(Duration.ofDays(30)));
            // conditions
            // act
            RefreshSessionLifetimeDecision result = policy.decideForRotation(record);
            // assert
            assertThat(result.getStatus()).isEqualTo(RefreshSessionLifetimeStatus.ABSOLUTE_EXPIRED);
            assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(result.getAbsoluteExpiresAt()).isEqualTo(CREATED_AT.plus(Duration.ofDays(30)));
            // verify
        }

        @Test
        @DisplayName("permits refresh one instant before idle deadline")
        void permitsRefreshOneInstantBeforeIdleDeadline() {
            // arrange
            Instant lastUsedAt = CREATED_AT.plus(Duration.ofDays(2));
            RefreshSessionFamilyRecord record = recordAt(lastUsedAt, 2L);
            Instant clockNow = lastUsedAt.plus(Duration.ofDays(7)).minusNanos(1);
            RefreshSessionLifetimeDecider policy = policyAt(clockNow);
            // conditions
            // act
            RefreshSessionLifetimeDecision result = policy.decideForRotation(record);
            // assert
            assertThat(result.getStatus()).isEqualTo(RefreshSessionLifetimeStatus.ROTATABLE);
            assertThat(result.getLastUsedAt()).isEqualTo(clockNow);
            // verify
        }

    }

    private static RefreshSessionLifetimeDecider policyAt(Instant instant) {
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        return new RefreshSessionLifetimeDecider(lifecycleProperties(), clock);
    }

    private static RefreshSessionFamilyRecord recordAt(Instant lastUsedAt, long rotationCounter) {
        Instant absoluteExpiresAt = CREATED_AT.plus(Duration.ofDays(30));
        Instant idleExpiresAt = lastUsedAt.plus(Duration.ofDays(7));
        if (idleExpiresAt.isAfter(absoluteExpiresAt)) {
            idleExpiresAt = absoluteExpiresAt;
        }

        return RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId("c12c7988-b9fe-4299-a72f-e430d004d37b")
                .instanceId("91ff9854-e8ac-4bf5-91d8-3117dcd8d05c")
                .roles(List.of("USER"))
                .authVersion(4L)
                .currentRefreshTokenHash("a".repeat(64))
                .rotationCounter(rotationCounter)
                .createdAt(CREATED_AT)
                .lastUsedAt(lastUsedAt)
                .idleExpiresAt(idleExpiresAt)
                .absoluteExpiresAt(absoluteExpiresAt)
                .deviceLabel("Firefox on macOS")
                .clientBindingHash("b".repeat(64))
                .build();
    }

}
