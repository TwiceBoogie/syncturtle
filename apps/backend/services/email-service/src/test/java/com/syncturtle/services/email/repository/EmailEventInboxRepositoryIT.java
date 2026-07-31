package com.syncturtle.services.email.repository;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.SECOND_EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.ERROR_MESSAGE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.PROCESSING_LEASE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.magicCodeCreateParam;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.model.param.EmailEventInboxRetryFailureParam;
import com.syncturtle.services.email.support.clock.TestClocks;
import com.syncturtle.testing.annotation.JpaIntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

@JpaIntegrationTest
@UsePostgresDb("email_service_repository_it")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("EmailEventInboxRepository")
class EmailEventInboxRepositoryIT {

    @Autowired
    EmailEventInboxRepository repository;
    @Autowired
    PlatformTransactionManager transactionManager;

    private ExecutorService executor;

    @BeforeEach
    void setup() {
        repository.deleteAllInBatch();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void cleanup() {
        executor.shutdownNow();
    }

    @Nested
    @DisplayName("findByEventId(String)")
    class FindByEventIdTests {

        @Test
        @DisplayName("finds persisted row by external event identifier")
        void findsPersistedRowByExternalEventIdentifier() {
            // arrange
            EmailEventInbox row = persistProcessing(EVENT_ID, PROCESSING_LEASE);
            // conditions
            // act
            EmailEventInbox found = repository.findByEventId(EVENT_ID).orElseThrow();
            // assert
            assertThat(found.getId()).isEqualTo(row.getId());
            assertThat(found.getEventId()).isEqualTo(EVENT_ID);
            // verify
        }

        @Test
        @DisplayName("returns empty for unknown event identifier")
        void returnsEmptyForUnknownEventIdentifier() {
            // arrange
            // conditions
            // act + assert
            assertThat(repository.findByEventId("missing-event")).isEmpty();
            // verify
        }

    }

    @Nested
    @DisplayName("findByEventIdForUpdate(String)")
    class FindByEventIdForUpdateTests {

        @Test
        @DisplayName("loads row through pessimistic write query")
        void loadsRowThroughPessimisticWriteQuery() {
            // arrange
            persistProcessing(EVENT_ID, PROCESSING_LEASE);
            TransactionTemplate transaction = transactionTemplate();
            // conditions
            // act
            EmailEventInbox found = transaction
                    .execute(status -> repository.findByEventIdForUpdate(EVENT_ID).orElseThrow());
            // assert
            assertThat(found).isNotNull();
            assertThat(found.getEventId()).isEqualTo(EVENT_ID);
            // verify
        }

    }

    @Nested
    @DisplayName("lockDueRetryIds(Instant, int)")
    class LockDueRetryIdsTests {

        @Test
        @DisplayName("returns only retries due at query time in schedule order")
        void returnsOnlyRetriesDueAtQueryTimeInScheduleOrder() {
            // arrange
            EmailEventInbox first = persistRetryable(EVENT_ID, Duration.ofSeconds(10));
            EmailEventInbox second = persistRetryable(SECOND_EVENT_ID, Duration.ofSeconds(20));
            persistRetryable("email-event-not-due", Duration.ofMinutes(5));
            Instant queryTime = TestClocks.NOW.plusSeconds(30);
            // conditions
            // act
            List<UUID> ids = transactionTemplate().execute(status -> repository.lockDueRetryIds(queryTime, 10));
            // assert
            assertThat(ids).containsExactly(first.getId(), second.getId());
            // verify
        }

        @Test
        @DisplayName("respects requested row limit")
        void respectsRequestedRowLimit() {
            // arrange
            persistRetryable(EVENT_ID, Duration.ofSeconds(10));
            persistRetryable(SECOND_EVENT_ID, Duration.ofSeconds(20));
            Instant queryTime = TestClocks.NOW.plusSeconds(30);
            // conditions
            // act
            List<UUID> ids = transactionTemplate().execute(status -> repository.lockDueRetryIds(queryTime, 1));
            // assert
            assertThat(ids).hasSize(1);
            // verify
        }

        @Test
        @DisplayName("skips row locked by competing transaction")
        void skipsRowLockedByCompetingTransaction() throws Exception {
            // arrange
            EmailEventInbox row = persistRetryable(EVENT_ID, Duration.ofSeconds(10));
            Instant queryTime = TestClocks.NOW.plusSeconds(30);
            CountDownLatch lockAcquired = new CountDownLatch(1);
            CountDownLatch releaseLock = new CountDownLatch(1);
            // conditions
            Future<?> lockHolder = executor.submit(() -> transactionTemplate().executeWithoutResult(status -> {
                List<UUID> locked = repository.lockDueRetryIds(queryTime, 1);
                assertThat(locked).containsExactly(row.getId());
                lockAcquired.countDown();
                await(releaseLock);
            }));

            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();
            // act
            List<UUID> competingResult = transactionTemplate()
                    .execute(status -> repository.lockDueRetryIds(queryTime, 1));
            // assert
            assertThat(competingResult).isEmpty();
            // verify
            releaseLock.countDown();
            lockHolder.get(5, TimeUnit.SECONDS);
        }

    }

    @Nested
    @DisplayName("lockExpiredProcessingIds(Instant, int)")
    class LockExpiredProcessingIdsTests {

        @Test
        @DisplayName("returns expired processing rows and ignores active leases")
        void returnsExpiredProcessingRowsAndIgnoresActiveLeases() {
            // arrange
            EmailEventInbox expired = persistProcessing(EVENT_ID, Duration.ofSeconds(10));
            persistProcessing(SECOND_EVENT_ID, Duration.ofMinutes(5));
            Instant queryTime = TestClocks.NOW.plusSeconds(30);
            // conditions
            // act
            List<UUID> ids = transactionTemplate()
                    .execute(status -> repository.lockExpiredProcessingIds(queryTime, 10));
            // assert
            assertThat(ids).containsExactly(expired.getId());
            // verify
        }

        @Test
        @DisplayName("respects requested row limit")
        void respectsRequestedRowLimit() {
            // arrange
            persistProcessing(EVENT_ID, Duration.ofSeconds(10));
            persistProcessing(SECOND_EVENT_ID, Duration.ofSeconds(15));
            Instant queryTime = TestClocks.NOW.plusSeconds(30);
            // conditions
            // act
            List<UUID> ids = transactionTemplate().execute(status -> repository.lockExpiredProcessingIds(queryTime, 1));
            // assert
            assertThat(ids).hasSize(1);
            // verify
        }

    }

    private EmailEventInbox persistProcessing(String eventId, Duration lease) {
        EmailEventInbox row = EmailEventInbox.create(magicCodeCreateParam(eventId), TestClocks.fixedUtc(), lease);
        return repository.saveAndFlush(row);
    }

    private EmailEventInbox persistRetryable(String eventId, Duration retryDelay) {
        EmailEventInbox row = persistProcessing(eventId, PROCESSING_LEASE);
        row.markRetryableFailure(new EmailEventInboxRetryFailureParam(5, retryDelay, ERROR_MESSAGE),
                TestClocks.fixedUtc());
        return repository.saveAndFlush(row);
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out awaiting test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while awaiting test latch", exception);
        }
    }

}