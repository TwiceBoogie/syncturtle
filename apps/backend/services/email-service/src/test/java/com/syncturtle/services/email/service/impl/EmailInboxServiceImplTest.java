package com.syncturtle.services.email.service.impl;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeEmailEvent;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.BATCH_SIZE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.inboxProperties;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.service.EmailInboxService;
import com.syncturtle.services.email.service.inbox.EmailInboxProcessor;
import com.syncturtle.services.email.service.inbox.EmailInboxStore;
import com.syncturtle.services.email.service.result.EmailInboxAcquireResult;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailInboxService")
class EmailInboxServiceImplTest {

    @Mock
    EmailInboxStore inboxStore;
    @Mock
    EmailInboxProcessor inboxProcessor;

    private EmailInboxService service;

    @BeforeEach
    void setup() {
        service = new EmailInboxServiceImpl(inboxStore, inboxProcessor, inboxProperties());
    }

    @Nested
    @DisplayName("receive(EmailToSendEvent)")
    class ReceiveTests {

        @Test
        @DisplayName("processes freshly acquired inbox row")
        void processesFreshlyAcquiredInboxRow() {
            // arrange
            EmailToSendEvent event = magicCodeEmailEvent();
            EmailEventInbox row = mock(EmailEventInbox.class);
            // conditions
            when(inboxStore.insertFresh(event)).thenReturn(EmailInboxAcquireResult.acquired(row));
            // act
            service.receive(event);
            // assert
            // verify
            verify(inboxProcessor).process(row);
        }

        @Test
        @DisplayName("resolves existing row after dupe exception")
        void resovlesExistingRowAfterDupeException() {
            // arrange
            EmailToSendEvent event = magicCodeEmailEvent();
            EmailEventInbox row = mock(EmailEventInbox.class);
            // conditions
            when(inboxStore.insertFresh(event)).thenThrow(new DataIntegrityViolationException("duplicate"));
            when(inboxStore.resolveExisting(EVENT_ID)).thenReturn(EmailInboxAcquireResult.acquired(row));
            // act
            service.receive(event);
            // assert
            // verify
            verify(inboxStore).resolveExisting(EVENT_ID);
            verify(inboxProcessor).process(row);
        }

        @Test
        @DisplayName("does not process duplicate already sent row")
        void doesNotProcessDuplicateAlreadySentRow() {
            // arrange
            EmailToSendEvent event = magicCodeEmailEvent();
            EmailEventInbox row = mock(EmailEventInbox.class);
            // conditions
            when(inboxStore.insertFresh(event)).thenReturn(EmailInboxAcquireResult.alreadySent(row));
            // act
            service.receive(event);
            // verify
            verifyNoInteractions(inboxProcessor);
        }

        @Test
        @DisplayName("rejects null event")
        void rejectsNullEvent() {
            // arrange
            // conditions
            // act
            assertThatThrownBy(() -> service.receive(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("email event is required");
            // assert
            // verify
            verifyNoInteractions(inboxStore, inboxProcessor);
        }

    }

    @Nested
    @DisplayName("retryDueEmails()")
    class RetryDueEmailsTests {

        @Test
        @DisplayName("returns without processing when no rows are due")
        void returnsWithoutProcessingWhenNoRowsAreDue() {
            // arrange
            // conditions
            when(inboxStore.acquireDueRetries(BATCH_SIZE)).thenReturn(List.of());
            // act
            service.retryDueEmails();
            // assert
            // verify
            verifyNoInteractions(inboxProcessor);
        }

        @Test
        @DisplayName("processes every acquired retry row")
        void processesEveryAcquiredRetryRow() {
            // arrange
            EmailEventInbox first = mock(EmailEventInbox.class);
            EmailEventInbox second = mock(EmailEventInbox.class);
            // conditions
            when(inboxStore.acquireDueRetries(BATCH_SIZE)).thenReturn(List.of(first, second));
            // act
            service.retryDueEmails();
            // assert
            // verify
            verify(inboxProcessor).process(first);
            verify(inboxProcessor).process(second);
        }

    }

}
