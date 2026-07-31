package com.syncturtle.services.email.service.collaborator;

import static com.syncturtle.services.email.support.assertion.EmailExceptionAssert.assertThatEmailExceptionThrownBy;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.RECIPIENTS;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeEmailEvent;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.BATCH_SIZE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.ERROR_MESSAGE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.PROCESSING_LEASE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.RECIPIENT_TO_JSON;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.TEMPLATE_MODEL_JSON;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.inboxProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.model.param.EmailEventInboxRetryFailureParam;
import com.syncturtle.services.email.repository.EmailEventInboxRepository;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.result.EmailInboxAcquireResult;
import com.syncturtle.services.email.support.clock.TestClocks;
import com.syncturtle.services.email.support.fixture.EmailFixtures.MagicCode;
import com.syncturtle.services.email.type.EmailInboxAcquireDecision;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailInboxStore")
class EmailInboxStoreTest {

    @Mock
    EmailEventInboxRepository repository;
    @Captor
    ArgumentCaptor<EmailEventInbox> rowCaptor;
    @Captor
    ArgumentCaptor<EmailEventInboxRetryFailureParam> retryParamCaptor;

    private JsonMapper jsonMapper;
    private EmailInboxStore store;

    @BeforeEach
    void setup() {
        jsonMapper = JsonMapper.builder().build();
        store = new EmailInboxStore(repository, inboxProperties(), jsonMapper, TestClocks.fixedUtc());
    }

    @Nested
    @DisplayName("insertFresh(EmailToSendEvent)")
    class InsertFreshTests {

        @Test
        @DisplayName("serializes event snapshot and saves processing row")
        void serializesEventSnapshotAndSavesProcessingRow() {
            // arrange
            // conditions
            // act
            EmailInboxAcquireResult result = store.insertFresh(magicCodeEmailEvent());
            // assert
            // verify + capture
            verify(repository).saveAndFlush(rowCaptor.capture());
            EmailEventInbox saved = rowCaptor.getValue();
            assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
            assertThat(saved.getRecipientToJson()).isEqualTo(RECIPIENT_TO_JSON);
            assertThat(jsonMapper.readTree(saved.getTemplateModelJson()))
                    .isEqualTo(jsonMapper.readTree(TEMPLATE_MODEL_JSON));
            assertThat(saved.getLockUntil()).isEqualTo(TestClocks.NOW.plus(PROCESSING_LEASE));
            assertThat(result.getDecision()).isEqualTo(EmailInboxAcquireDecision.ACQUIRED);
            assertThat(result.getRow()).isSameAs(saved);
        }

        @Test
        @DisplayName("rejects null event before persistence")
        void rejectsNullEventBeforePersistence() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> store.insertFresh(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("email event is required");
            // verify
            verifyNoInteractions(repository);
        }

    }

    @Nested
    @DisplayName("resolveExisting(String)")
    class ResolveExistingTests {

        @Test
        @DisplayName("returns already sent decision without saving")
        void returnsAlreadySentDecisionWithoutSaving() {
            // arrange
            EmailEventInbox row = mock(EmailEventInbox.class);
            // conditions
            when(repository.findByEventIdForUpdate(EVENT_ID)).thenReturn(Optional.of(row));
            when(row.isSent()).thenReturn(true);
            // act
            EmailInboxAcquireResult result = store.resolveExisting(" " + EVENT_ID + " ");
            // assert
            assertThat(result.getDecision()).isEqualTo(EmailInboxAcquireDecision.ALREADY_SENT);
            // verify
            verify(repository).findByEventIdForUpdate(EVENT_ID);
            verify(row).isSent();
            verifyNoMoreInteractions(row, repository);
        }

        @Test
        @DisplayName("returns scheduled retry decision before due time")
        void returnsScheduledRetryDecisionBeforeDueTime() {
            // arrange
            EmailEventInbox row = mock(EmailEventInbox.class);
            // conditions
            when(repository.findByEventIdForUpdate(EVENT_ID)).thenReturn(Optional.of(row));
            when(row.isSent()).thenReturn(false);
            when(row.isPermanentFailure()).thenReturn(false);
            when(row.hasActiveProcessingLease(TestClocks.fixedUtc())).thenReturn(false);
            when(row.hasScheduledRetry(TestClocks.fixedUtc())).thenReturn(true);
            // act
            EmailInboxAcquireResult result = store.resolveExisting(EVENT_ID);
            // assert
            assertThat(result.getDecision()).isEqualTo(EmailInboxAcquireDecision.RETRY_SCHEDULED);
            // verify
        }

    }

    @Nested
    @DisplayName("acquireDueRetries(int)")
    class AcquireDueRetriesTests {

        @Test
        @DisplayName("returns empty list when no row IDs are due")
        void returnsEmptyListWhenNoRowIdsAreDue() {
            // arrange
            // conditions
            when(repository.lockDueRetryIds(TestClocks.NOW, BATCH_SIZE)).thenReturn(List.of());
            when(repository.lockExpiredProcessingIds(TestClocks.NOW, BATCH_SIZE)).thenReturn(List.of());
            // act + assert
            assertThat(store.acquireDueRetries(BATCH_SIZE)).isEmpty();
            // verify
            verify(repository, never()).findAllById(any());
        }

        @Test
        @DisplayName("claims rows returned by retry and expired lease queries")
        void claimsRowsReturnedByRetryAndExpiredLeaseQueries() {
            // arrange
            UUID firstId = UUID.randomUUID();
            UUID secondId = UUID.randomUUID();
            EmailEventInbox first = mock(EmailEventInbox.class);
            EmailEventInbox second = mock(EmailEventInbox.class);
            // conditions
            when(first.getId()).thenReturn(firstId);
            when(second.getId()).thenReturn(secondId);
            when(repository.lockDueRetryIds(TestClocks.NOW, BATCH_SIZE)).thenReturn(List.of(firstId));
            when(repository.lockExpiredProcessingIds(TestClocks.NOW, BATCH_SIZE - 1)).thenReturn(List.of(secondId));
            when(repository.findAllById(List.of(firstId, secondId))).thenReturn(List.of(first, second));
            when(repository.saveAllAndFlush(ArgumentMatchers.anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            // act
            List<EmailEventInbox> result = store.acquireDueRetries(BATCH_SIZE);
            // assert
            assertThat(result).containsExactlyInAnyOrder(first, second);
            // verify
            verify(first).claimForProcessing(TestClocks.fixedUtc(), PROCESSING_LEASE);
            verify(second).claimForProcessing(TestClocks.fixedUtc(), PROCESSING_LEASE);
        }

    }

    @Nested
    @DisplayName("toDispatchParam(EmailEventInbox")
    class ToDispatchParamTests {

        @Test
        @DisplayName("deserializesStoredSnapshotIntoDispatchParameter")
        void deserializesStoredSnapshotIntoDispatchParameter() {
            // arrange
            EmailEventInbox row = mock(EmailEventInbox.class);
            // conditions
            when(row.getEventId()).thenReturn(EVENT_ID);
            when(row.getTemplateType()).thenReturn(EmailTemplateType.MAGIC_CODE);
            when(row.getSubject()).thenReturn(MagicCode.SUBJECT);
            when(row.getRecipientToJson()).thenReturn(RECIPIENT_TO_JSON);
            when(row.getTemplateModelJson()).thenReturn(TEMPLATE_MODEL_JSON);
            // act
            EmailDispatchParam result = store.toDispatchParam(row);
            // assert
            assertThat(result.getRecipients()).isEqualTo(RECIPIENTS);
            assertThat(result.getModel()).containsEntry("code", MagicCode.CODE);
            // verify
        }

        @Test
        @DisplayName("wraps malformed stored JSON")
        void wrapsMalformedStoredJson() {
            // arrange
            EmailEventInbox row = mock(EmailEventInbox.class);
            // conditions
            when(row.getEventId()).thenReturn(EVENT_ID);
            when(row.getRecipientToJson()).thenReturn("not-json");
            // act + assert
            assertThatEmailExceptionThrownBy(() -> store.toDispatchParam(row))
                    .hasErrorCode(EmailErrorCode.EMAIL_INBOX_PAYLOAD_DESERIALIZATION_FAILED);
            // verify
        }

    }

    @Nested
    @DisplayName("markSent(String)")
    class MarkSentTests {

        @Test
        @DisplayName("loads locked row, applies transition, and flushes")
        void loadsLockedRowAppliesTransitionAndFlushes() {
            // arrange
            EmailEventInbox row = mock(EmailEventInbox.class);
            // conditions
            when(repository.findByEventIdForUpdate(EVENT_ID)).thenReturn(Optional.of(row));
            // act
            store.markSent(EVENT_ID);
            // assert
            // verify
            verify(row).markSent(TestClocks.fixedUtc());
            verify(repository).saveAndFlush(row);
        }

    }

    @Nested
    @DisplayName("markRetryableFailure(String, Exception)")
    class MarkRetryableFailureTests {

        @Test
        @DisplayName("builds exponential retry parameter from current attempt")
        void buildsExponentialRetryParameterFromCurrentAttempt() {
            // arrange
            EmailEventInbox row = mock(EmailEventInbox.class);
            // conditions
            when(repository.findByEventIdForUpdate(EVENT_ID)).thenReturn(Optional.of(row));
            when(row.getAttemptCount()).thenReturn(2);
            // act
            store.markRetryableFailure(EVENT_ID, new RuntimeException(ERROR_MESSAGE));
            // assert
            // verify + capture
            verify(row).markRetryableFailure(retryParamCaptor.capture(), ArgumentMatchers.any());
            assertThat(retryParamCaptor.getValue().getRetryDelay()).isEqualTo(Duration.ofSeconds(60));
            verify(repository).saveAndFlush(row);
        }

    }

    @Nested
    @DisplayName("markPermanentFailure(String, Exception)")
    class MarkPermanentFailureTests {

        @Test
        @DisplayName("summarizes root cause and applies permanent transition")
        void summarizesRootCauseAndAppliesPermanentTransition() {
            // arrange
            EmailEventInbox row = mock(EmailEventInbox.class);
            RuntimeException root = new RuntimeException("connection refused");
            RuntimeException wrapper = new RuntimeException("wrapper", root);
            // conditions
            when(repository.findByEventIdForUpdate(EVENT_ID)).thenReturn(Optional.of(row));
            // act
            store.markPermanentFailure(EVENT_ID, wrapper);
            // assert
            // verify
            verify(row).markPermanentFailure("RuntimeException: connection refused", TestClocks.fixedUtc());
            verify(repository).saveAndFlush(row);
        }
    }

}
