package com.syncturtle.services.email.model;

import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.ERROR_MESSAGE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.PROCESSING_LEASE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.RETRY_DELAY;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.createParam;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.retryFailureParam;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            EmailEventInbox row = EmailEventInbox.create(createParam(), TestClocks.fixedUtc(), PROCESSING_LEASE);

            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.PROCESSING);
            assertThat(row.getAttemptCount()).isEqualTo(1);
            assertThat(row.getLockUntil()).isEqualTo(TestClocks.NOW.plus(PROCESSING_LEASE));
            assertThat(row.getCreatedAt()).isEqualTo(TestClocks.NOW);
            assertThat(row.getUpdatedAt()).isEqualTo(TestClocks.NOW);
            assertThat(row.getProcessedAt()).isNull();
            assertThat(row.getNextAttemptAt()).isNull();
        }

        @Test
        @DisplayName("rejects non-positive processing lease")
        void rejectsNonPositiveProcessingLease() {
            assertThatThrownBy(() -> EmailEventInbox.create(createParam(), TestClocks.fixedUtc(), Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("processingLease must be positive");
        }

    }

    @Nested
    @DisplayName("hasActiveProcessingLease(Clock)")
    class HasActiveProcessingLeaseTests {

        @Test
        @DisplayName("returns true before lease expires and false afterward")
        void returnsTrueBeforeLeaseExpiresAndFalseAfterward() {
            MutableClock clock = TestClocks.mutableUtc();
            EmailEventInbox row = EmailEventInbox.create(createParam(), clock, PROCESSING_LEASE);

            assertThat(row.hasActiveProcessingLease(clock)).isTrue();
            clock.advance(PROCESSING_LEASE.plusMillis(1));
            assertThat(row.hasActiveProcessingLease(clock)).isFalse();
        }
    }

    @Nested
    @DisplayName("claimForProcessing(Clock, Duration)")
    class ClaimForProcessingTests {

        @Test
        @DisplayName("reclaims expired processing row and increments attempt")
        void reclaimsExpiredProcessingRowAndIncrementsAttempt() {
            MutableClock clock = TestClocks.mutableUtc();
            EmailEventInbox row = EmailEventInbox.create(createParam(), clock, PROCESSING_LEASE);
            clock.advance(PROCESSING_LEASE.plusSeconds(1));

            row.claimForProcessing(clock, PROCESSING_LEASE);

            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.PROCESSING);
            assertThat(row.getAttemptCount()).isEqualTo(2);
            assertThat(row.getLockUntil()).isEqualTo(clock.instant().plus(PROCESSING_LEASE));
        }

        @Test
        @DisplayName("rejects claim while processing lease is active")
        void rejectsClaimWhileProcessingLeaseIsActive() {
            MutableClock clock = TestClocks.mutableUtc();
            EmailEventInbox row = EmailEventInbox.create(createParam(), clock, PROCESSING_LEASE);

            assertThatThrownBy(() -> row.claimForProcessing(clock, PROCESSING_LEASE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Email inbox processing lease is still active");
        }
    }

    @Nested
    @DisplayName("markSent(Clock)")
    class MarkSentTests {

        @Test
        @DisplayName("marks processing row sent and releases scheduling state")
        void marksProcessingRowSentAndReleasesSchedulingState() {
            EmailEventInbox row = EmailEventInbox.create(createParam(), TestClocks.fixedUtc(), PROCESSING_LEASE);

            row.markSent(TestClocks.fixedUtc());

            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.SENT);
            assertThat(row.getProcessedAt()).isEqualTo(TestClocks.NOW);
            assertThat(row.getLockUntil()).isNull();
            assertThat(row.getNextAttemptAt()).isNull();
            assertThat(row.getLastError()).isNull();
        }
    }

    @Nested
    @DisplayName("markRetryableFailure(EmailEventInboxRetryFailureParam, Clock)")
    class MarkRetryableFailureTests {

        @Test
        @DisplayName("schedules next attempt before maximum attempts")
        void schedulesNextAttemptBeforeMaximumAttempts() {
            EmailEventInbox row = EmailEventInbox.create(createParam(), TestClocks.fixedUtc(), PROCESSING_LEASE);

            row.markRetryableFailure(retryFailureParam(), TestClocks.fixedUtc());

            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_RETRYABLE);
            assertThat(row.getNextAttemptAt()).isEqualTo(TestClocks.NOW.plus(RETRY_DELAY));
            assertThat(row.getProcessedAt()).isNull();
            assertThat(row.getLastError()).isEqualTo(ERROR_MESSAGE);
        }

        @Test
        @DisplayName("marks permanent failure when attempt limit is reached")
        void marksPermanentFailureWhenAttemptLimitIsReached() {
            MutableClock clock = TestClocks.mutableUtc();
            EmailEventInbox row = EmailEventInbox.create(createParam(), clock, PROCESSING_LEASE);
            clock.advance(PROCESSING_LEASE.plusSeconds(1));
            row.claimForProcessing(clock, PROCESSING_LEASE);

            EmailEventInboxRetryFailureParam param = new EmailEventInboxRetryFailureParam(2, RETRY_DELAY,
                    ERROR_MESSAGE);
            row.markRetryableFailure(param, clock);

            assertThat(row.getAttemptCount()).isEqualTo(2);
            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_PERMANENT);
            assertThat(row.getProcessedAt()).isEqualTo(clock.instant());
            assertThat(row.getNextAttemptAt()).isNull();
        }
    }

    @Nested
    @DisplayName("markPermanentFailure(String, Clock)")
    class MarkPermanentFailureTests {

        @Test
        @DisplayName("marks processing row permanently failed")
        void marksProcessingRowPermanentlyFailed() {
            EmailEventInbox row = EmailEventInbox.create(createParam(), TestClocks.fixedUtc(), PROCESSING_LEASE);

            row.markPermanentFailure(ERROR_MESSAGE, TestClocks.fixedUtc());

            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_PERMANENT);
            assertThat(row.getProcessedAt()).isEqualTo(TestClocks.NOW);
            assertThat(row.getLastError()).isEqualTo(ERROR_MESSAGE);
        }
    }

}
