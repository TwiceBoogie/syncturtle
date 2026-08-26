package com.syncturtle.services.email.service.collaborator;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.SECOND_EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.THIRD_EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeEmailEvent;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.ERROR_MESSAGE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.RETRY_DELAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.services.email.configuration.property.EmailInboxProperties;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.repository.EmailEventInboxRepository;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.result.EmailInboxAcquireResult;
import com.syncturtle.services.email.support.clock.MutableClock;
import com.syncturtle.services.email.support.clock.TestClocks;
import com.syncturtle.services.email.support.fixture.EmailFixtures.MagicCode;
import com.syncturtle.services.email.type.EmailEventInboxStatus;
import com.syncturtle.services.email.type.EmailInboxAcquireDecision;
import com.syncturtle.testing.annotation.JpaIntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

import tools.jackson.databind.json.JsonMapper;

@JpaIntegrationTest
@UsePostgresDb("email_service_inbox_store_it")
@Import({
        EmailInboxStore.class,
        EmailInboxStoreIT.StoreTestConfiguration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("EmailInboxStore")
class EmailInboxStoreIT {

    @Autowired
    EmailInboxStore store;
    @Autowired
    EmailEventInboxRepository repository;
    @Autowired
    MutableClock clock;
    @Autowired
    EmailInboxProperties properties;

    @BeforeEach
    void setup() {
        repository.deleteAllInBatch();
        clock.setInstant(TestClocks.NOW);
    }

    @Nested
    @DisplayName("insertFresh(EmailToSendEvent)")
    class InsertFreshTests {

        @Test
        @DisplayName("persists immutable events snapshot and processing lease")
        void persistsImmutableEventsSnapshotAndProcessingLease() {
            // arrange
            // conditions
            // act
            EmailInboxAcquireResult result = store.insertFresh(magicCodeEmailEvent());
            // assert
            EmailEventInbox persisted = repository.findByEventId(EVENT_ID).orElseThrow();
            assertThat(result.getDecision()).isEqualTo(EmailInboxAcquireDecision.ACQUIRED);
            assertThat(persisted.getStatus()).isEqualTo(EmailEventInboxStatus.PROCESSING);
            assertThat(persisted.getAttemptCount()).isEqualTo(1);
            assertThat(persisted.getLockUntil()).isEqualTo(TestClocks.NOW.plus(properties.getProcessing().getLease()));
            assertThat(persisted.getRecipientToJson()).contains("lunasnow@marvel.com");
            assertThat(persisted.getTemplateModelJson()).contains("code");
            // verify
        }

        @Test
        @DisplayName("enforces unique event identifier through db constraint")
        void enforcesUniqueEventIdentifierThroughDbConstraint() {
            // arrange
            EmailToSendEvent event = magicCodeEmailEvent();
            store.insertFresh(event);
            // conditions
            // act + assert
            assertThatThrownBy(() -> store.insertFresh(magicCodeEmailEvent()))
                    .isInstanceOf(DataIntegrityViolationException.class);
            // verify
            assertThat(repository.count()).isEqualTo(1);
        }

    }

    @Nested
    @DisplayName("resolveExisting(String)")
    class ResolveExistingTests {

        @Test
        @DisplayName("returns in progress while processing lease is active")
        void returnsInProgressWhileProcessingLeaseIsActive() {
            // arrange
            store.insertFresh(magicCodeEmailEvent());
            // conditions
            // act
            EmailInboxAcquireResult result = store.resolveExisting(" " + EVENT_ID + " ");
            // assert
            assertThat(result.getDecision()).isEqualTo(EmailInboxAcquireDecision.IN_PROGRESS);
            assertThat(result.getRow().getAttemptCount()).isEqualTo(1);
            assertThat(result.getRow().getLockUntil())
                    .isEqualTo(clock.instant().plus(properties.getProcessing().getLease()));
            // verify
        }

        @Test
        @DisplayName("reclaims processing row after lease expires")
        void reclaimsProcessingRowAfterLeaseExpires() {
            // arrange
            store.insertFresh(magicCodeEmailEvent());
            clock.advance(properties.getProcessing().getLease().plusNanos(1));
            // conditions
            // act
            EmailInboxAcquireResult result = store.resolveExisting(EVENT_ID);
            // assert
            assertThat(result.getDecision()).isEqualTo(EmailInboxAcquireDecision.ACQUIRED);
            assertThat(result.getRow().getAttemptCount()).isEqualTo(2);
            assertThat(result.getRow().getLockUntil())
                    .isEqualTo(clock.instant().plus(properties.getProcessing().getLease()));
            // verify
        }

        @Test
        @DisplayName("returns scheduled retry before due time and acquires it at due time")
        void returnsScheduledRetryBeforeDueTimeAndAcquiresItAtDueTime() {
            // arrange
            store.insertFresh(magicCodeEmailEvent());
            store.markRetryableFailure(EVENT_ID, new RuntimeException(ERROR_MESSAGE));
            // conditions
            // act
            EmailInboxAcquireResult scheduled = store.resolveExisting(EVENT_ID);
            // assert
            assertThat(scheduled.getDecision()).isEqualTo(EmailInboxAcquireDecision.RETRY_SCHEDULED);

            clock.advance(RETRY_DELAY);
            EmailInboxAcquireResult acquired = store.resolveExisting(EVENT_ID);
            assertThat(acquired.getDecision()).isEqualTo(EmailInboxAcquireDecision.ACQUIRED);
            assertThat(acquired.getRow().getAttemptCount()).isEqualTo(2);
            // verify
        }

        @Test
        @DisplayName("returns terminal decisions without changing persisted state")
        void returnsTerminalDecisionsWithoutChangingPersistedState() {
            // arrange
            store.insertFresh(magicCodeEmailEvent());
            store.markSent(EVENT_ID);

            store.insertFresh(magicCodeEmailEvent(SECOND_EVENT_ID));
            store.markPermanentFailure(SECOND_EVENT_ID, new RuntimeException(ERROR_MESSAGE));
            // conditions
            // act
            EmailInboxAcquireResult sent = store.resolveExisting(EVENT_ID);
            EmailInboxAcquireResult failed = store.resolveExisting(SECOND_EVENT_ID);
            // assert
            assertThat(sent.getDecision()).isEqualTo(EmailInboxAcquireDecision.ALREADY_SENT);
            assertThat(failed.getDecision()).isEqualTo(EmailInboxAcquireDecision.PERMANENT_FAILURE);
            assertThat(repository.findByEventId(EVENT_ID).orElseThrow().getAttemptCount()).isEqualTo(1);
            assertThat(repository.findByEventId(SECOND_EVENT_ID).orElseThrow().getAttemptCount()).isEqualTo(1);
            // verify
        }

    }

    @Nested
    @DisplayName("acquireDueRetries(int)")
    class AcquireDueRetriesTests {

        @Test
        @DisplayName("claims due retries and expired leases up to batch limit")
        void claimsDueRetriesAndExpiredLeasesUpToBatchLimit() {
            // arrange
            store.insertFresh(magicCodeEmailEvent());
            store.markRetryableFailure(EVENT_ID, new RuntimeException(ERROR_MESSAGE));

            store.insertFresh(magicCodeEmailEvent(SECOND_EVENT_ID));

            store.insertFresh(magicCodeEmailEvent(THIRD_EVENT_ID));
            store.markRetryableFailure(THIRD_EVENT_ID, new RuntimeException(ERROR_MESSAGE));
            clock.advance(properties.getProcessing().getLease().plusSeconds(1));
            // conditions
            // act
            List<EmailEventInbox> rows = store.acquireDueRetries(2);
            // assert
            assertThat(rows).hasSize(2);
            assertThat(rows).allSatisfy(row -> {
                assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.PROCESSING);
                assertThat(row.getAttemptCount()).isEqualTo(2);
                assertThat(row.getNextAttemptAt()).isNull();
            });
            // verify
        }

        @Test
        @DisplayName("returns empty when no retry or expired lease is eligible")
        void returnsEmptyWhenNoRetryOrExpiredLeaseIsEligible() {
            // arrange
            store.insertFresh(magicCodeEmailEvent());
            // conditions
            // act + assert
            assertThat(store.acquireDueRetries(10)).isEmpty();
            // verify
        }

        @Test
        @DisplayName("rejects non-positive batch limit before repository access")
        void rejectsNonPositiveBatchLimitBeforeRepositoryAccess() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> store.acquireDueRetries(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("limit must be at least 1");
            // verify
        }

    }

    @Nested
    @DisplayName("toDispatchParam(EmailEventInbox)")
    class ToDispatchParamTests {

        @Test
        @DisplayName("deserializes the exact persisted event snapshot")
        void deserializesTheExactPersistedEventSnapshot() {
            // arrange
            EmailEventInbox row = store.insertFresh(magicCodeEmailEvent()).getRow();
            // conditions
            // act
            EmailDispatchParam param = store.toDispatchParam(row);
            // assert
            assertThat(param.getEventId()).isEqualTo(EVENT_ID);
            assertThat(param.getRecipients()).containsExactly(
                    "lunasnow@marvel.com",
                    "suestorm@marvel.com");
            assertThat(param.getModel()).containsEntry("code", MagicCode.CODE);
            // verify
        }

    }

    @Nested
    @DisplayName("markSent(String)")
    class MarkSentTests {

        @Test
        @DisplayName("persists sent terminal transition")
        void persistsSentTerminalTransition() {
            // arrange
            store.insertFresh(magicCodeEmailEvent());
            // conditions
            // act
            store.markSent(EVENT_ID);
            // assert
            EmailEventInbox persisted = repository.findByEventId(EVENT_ID).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(EmailEventInboxStatus.SENT);
            assertThat(persisted.getProcessedAt()).isEqualTo(TestClocks.NOW);
            assertThat(persisted.getLockUntil()).isNull();
            // verify
        }

    }

    @Nested
    @DisplayName("markRetryableFailure(String, Exception)")
    class MarkRetryableFailureTests {

        @Test
        @DisplayName("persists retry schedule from current attempt count")
        void persistsRetryScheduleFromCurrentAttemptCount() {
            // arrange
            store.insertFresh(magicCodeEmailEvent());
            // conditions
            // act
            store.markRetryableFailure(EVENT_ID, new RuntimeException("connection refused"));
            // assert
            EmailEventInbox persisted = repository.findByEventId(EVENT_ID).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_RETRYABLE);
            assertThat(persisted.getNextAttemptAt()).isEqualTo(TestClocks.NOW.plus(RETRY_DELAY));
            assertThat(persisted.getLastError()).isEqualTo("RuntimeException: connection refused");
            // verify
        }

    }

    @Nested
    @DisplayName("markPermanentFailure(String, Exception)")
    class MarkPermanentFailureTests {

        @Test
        @DisplayName("persists root cause summary and truncates it to column length")
        void persistsRootCauseSummaryAndTruncatesItToColumnLength() {
            // arrange
            store.insertFresh(magicCodeEmailEvent());
            RuntimeException root = new RuntimeException("x".repeat(5_000));
            RuntimeException wrapper = new RuntimeException("wrapper", root);
            // conditions
            // act
            store.markPermanentFailure(EVENT_ID, wrapper);
            // assert
            EmailEventInbox persisted = repository.findByEventId(EVENT_ID).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_PERMANENT);
            assertThat(persisted.getLastError()).hasSize(4_000);
            assertThat(persisted.getLastError()).startsWith("RuntimeException: ");
            // verify
        }

    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmailInboxProperties.class)
    static class StoreTestConfiguration {

        @Bean
        MutableClock clock() {
            return TestClocks.mutableUtc();
        }

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

    }

}
