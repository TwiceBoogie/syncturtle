package com.syncturtle.services.email.model;

import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.ERROR_MESSAGE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.PROCESSING_LEASE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.RETRY_DELAY;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.magicCodeCreateParam;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.retryFailureParam;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.email.model.param.EmailEventInboxCreateParam;
import com.syncturtle.services.email.model.param.EmailEventInboxRetryFailureParam;
import com.syncturtle.services.email.support.clock.MutableClock;
import com.syncturtle.services.email.support.clock.TestClocks;
import com.syncturtle.services.email.type.EmailEventInboxStatus;

@DisplayName("EmailEventInbox")
class EmailEventInboxTest {

    @Nested
    @DisplayName("create(EmailEventInboxCreateParam, Clock, Duration)")
    class CreateTests {

        @Test
        @DisplayName("creates processing row with first attempt and active lease")
        void createsProcessingRowWithFirstAttemptAndActiveLease() {
            // arrange
            EmailEventInboxCreateParam param = magicCodeCreateParam();
            // conditions
            // act
            EmailEventInbox row = EmailEventInbox.create(param, TestClocks.fixedUtc(), PROCESSING_LEASE);
            // assert
            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.PROCESSING);
            assertThat(row.getAttemptCount()).isEqualTo(1);
            assertThat(row.getLockUntil()).isEqualTo(TestClocks.NOW.plus(PROCESSING_LEASE));
            assertThat(row.getCreatedAt()).isEqualTo(TestClocks.NOW);
            assertThat(row.getUpdatedAt()).isEqualTo(TestClocks.NOW);
            assertThat(row.getProcessedAt()).isNull();
            assertThat(row.getNextAttemptAt()).isNull();
            // verify
        }

        @Test
        @DisplayName("rejects non-positive processing lease")
        void rejectsNonPositiveProcessingLease() {
            // arrange
            EmailEventInboxCreateParam param = magicCodeCreateParam();
            // conditions
            // act + assert
            assertThatThrownBy(() -> EmailEventInbox.create(param, TestClocks.fixedUtc(), Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("processingLease must be positive");
            // verify
        }

    }

    @Nested
    @DisplayName("hasActiveProcessingLease(Clock)")
    class HasActiveProcessingLeaseTests {

        @Test
        @DisplayName("returns true before lease expires and false afterward")
        void returnsTrueBeforeLeaseExpiresAndFalseAfterward() {
            // arrange
            EmailEventInboxCreateParam param = magicCodeCreateParam();
            MutableClock clock = TestClocks.mutableUtc();
            // conditions
            // act
            EmailEventInbox row = EmailEventInbox.create(param, clock, PROCESSING_LEASE);
            // assert
            assertThat(row.hasActiveProcessingLease(clock)).isTrue();
            clock.advance(PROCESSING_LEASE.plusMillis(1));
            assertThat(row.hasActiveProcessingLease(clock)).isFalse();
            // verify
        }
    }

    @Nested
    @DisplayName("claimForProcessing(Clock, Duration)")
    class ClaimForProcessingTests {

        @Test
        @DisplayName("reclaims expired processing row and increments attempt")
        void reclaimsExpiredProcessingRowAndIncrementsAttempt() {
            // arrange
            EmailEventInboxCreateParam param = magicCodeCreateParam();
            MutableClock clock = TestClocks.mutableUtc();
            // conditions
            // act
            EmailEventInbox row = EmailEventInbox.create(param, clock, PROCESSING_LEASE);
            clock.advance(PROCESSING_LEASE.plusSeconds(1));

            row.claimForProcessing(clock, PROCESSING_LEASE);
            // assert
            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.PROCESSING);
            assertThat(row.getAttemptCount()).isEqualTo(2);
            assertThat(row.getLockUntil()).isEqualTo(clock.instant().plus(PROCESSING_LEASE));
            /// verify
        }

        @Test
        @DisplayName("rejects claim while processing lease is active")
        void rejectsClaimWhileProcessingLeaseIsActive() {
            // arrange
            EmailEventInboxCreateParam param = magicCodeCreateParam();
            MutableClock clock = TestClocks.mutableUtc();
            // conditions
            // act
            EmailEventInbox row = EmailEventInbox.create(param, clock, PROCESSING_LEASE);
            // assert
            assertThatThrownBy(() -> row.claimForProcessing(clock, PROCESSING_LEASE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Email inbox processing lease is still active");
            // verify
        }
    }

    @Nested
    @DisplayName("markSent(Clock)")
    class MarkSentTests {

        @Test
        @DisplayName("marks processing row sent and releases scheduling state")
        void marksProcessingRowSentAndReleasesSchedulingState() {
            // arrange
            EmailEventInboxCreateParam param = magicCodeCreateParam();
            // conditions
            // act
            EmailEventInbox row = EmailEventInbox.create(param, TestClocks.fixedUtc(), PROCESSING_LEASE);

            row.markSent(TestClocks.fixedUtc());
            // assert
            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.SENT);
            assertThat(row.getProcessedAt()).isEqualTo(TestClocks.NOW);
            assertThat(row.getLockUntil()).isNull();
            assertThat(row.getNextAttemptAt()).isNull();
            assertThat(row.getLastError()).isNull();
            /// verify
        }
    }

    @Nested
    @DisplayName("markRetryableFailure(EmailEventInboxRetryFailureParam, Clock)")
    class MarkRetryableFailureTests {

        @Test
        @DisplayName("schedules next attempt before maximum attempts")
        void schedulesNextAttemptBeforeMaximumAttempts() {
            // arrange
            EmailEventInboxCreateParam param = magicCodeCreateParam();
            // conditons
            // act
            EmailEventInbox row = EmailEventInbox.create(param, TestClocks.fixedUtc(), PROCESSING_LEASE);

            row.markRetryableFailure(retryFailureParam(), TestClocks.fixedUtc());
            // assert
            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_RETRYABLE);
            assertThat(row.getNextAttemptAt()).isEqualTo(TestClocks.NOW.plus(RETRY_DELAY));
            assertThat(row.getProcessedAt()).isNull();
            assertThat(row.getLastError()).isEqualTo(ERROR_MESSAGE);
            // verify
        }

        @Test
        @DisplayName("marks permanent failure when attempt limit is reached")
        void marksPermanentFailureWhenAttemptLimitIsReached() {
            // arrange
            MutableClock clock = TestClocks.mutableUtc();
            EmailEventInboxRetryFailureParam param = new EmailEventInboxRetryFailureParam(2, RETRY_DELAY,
                    ERROR_MESSAGE);
            // conditions
            // act
            EmailEventInbox row = EmailEventInbox.create(magicCodeCreateParam(), clock, PROCESSING_LEASE);
            clock.advance(PROCESSING_LEASE.plusSeconds(1));
            row.claimForProcessing(clock, PROCESSING_LEASE);

            row.markRetryableFailure(param, clock);
            // assert
            assertThat(row.getAttemptCount()).isEqualTo(2);
            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_PERMANENT);
            assertThat(row.getProcessedAt()).isEqualTo(clock.instant());
            assertThat(row.getNextAttemptAt()).isNull();
            // verify
        }
    }

    @Nested
    @DisplayName("markPermanentFailure(String, Clock)")
    class MarkPermanentFailureTests {

        @Test
        @DisplayName("marks processing row permanently failed")
        void marksProcessingRowPermanentlyFailed() {
            // arrange
            // conditons
            // act
            EmailEventInbox row = EmailEventInbox.create(magicCodeCreateParam(), TestClocks.fixedUtc(),
                    PROCESSING_LEASE);

            row.markPermanentFailure(ERROR_MESSAGE, TestClocks.fixedUtc());
            // assert
            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_PERMANENT);
            assertThat(row.getProcessedAt()).isEqualTo(TestClocks.NOW);
            assertThat(row.getLastError()).isEqualTo(ERROR_MESSAGE);
            // verify
        }
    }

}
