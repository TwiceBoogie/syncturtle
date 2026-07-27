package com.syncturtle.services.email.service.inbox;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_TYPE;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeDispatchParam;
import static com.syncturtle.services.email.exception.EmailInboxException.payloadDeserializationFailed;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.services.email.exception.EmailDispatchException;
import com.syncturtle.services.email.exception.EmailInboxException;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.service.dispatch.EmailDispatcher;
import com.syncturtle.services.email.service.param.EmailDispatchParam;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailInboxProcessor")
class EmailInboxProcessorTest {

    @Mock
    EmailInboxStore inboxStore;
    @Mock
    EmailDispatcher emailDispatcher;

    @Mock
    EmailEventInbox row;

    private EmailInboxProcessor processor;

    @BeforeEach
    void setup() {
        processor = new EmailInboxProcessor(inboxStore, emailDispatcher);
    }

    @Nested
    @DisplayName("process(EmailEventInbox")
    class ProcessTests {

        @Test
        @DisplayName("dispatches and marks row sent")
        void dispatchesAndMarksRowSent() {
            // arrange
            EmailDispatchParam param = magicCodeDispatchParam();
            // conditions
            stubRow();
            when(inboxStore.toDispatchParam(row)).thenReturn(param);
            // act
            processor.process(row);
            // assert
            // verify
            verify(emailDispatcher).dispatch(param);
            verify(inboxStore).markSent(EVENT_ID);
        }

        @Test
        @DisplayName("marks malformed inbox payload permanently failed")
        void marksMalformedInboxPayloadPermanentlyFailed() {
            // arrange
            EmailInboxException exception = payloadDeserializationFailed(EVENT_ID,
                    new RuntimeException("invalid JSON"));
            // conditions
            stubRow();
            when(inboxStore.toDispatchParam(row)).thenThrow(exception);
            // act
            processor.process(row);
            // assert
            // verify
            verify(inboxStore).markPermanentFailure(EVENT_ID, exception);
            verifyNoInteractions(emailDispatcher);
        }

        @Test
        @DisplayName("marks retryable dispatch exception for retry")
        void marksRetryableDispatchExceptionForRetry() {
            // arrange
            EmailDispatchException exception = mock(EmailDispatchException.class);
            EmailDispatchParam param = magicCodeDispatchParam();
            // conditions
            stubRow();
            when(exception.isRetryable()).thenReturn(true);
            when(inboxStore.toDispatchParam(row)).thenReturn(param);
            doThrow(exception).when(emailDispatcher).dispatch(param);
            // act
            processor.process(row);
            // assert
            // verify
            verify(inboxStore).markRetryableFailure(EVENT_ID, exception);
        }

        @Test
        @DisplayName("marks nonretryable dispatch exception permanently failed")
        void marksNonRetryableDispatchExceptionPermanentlyFailed() {
            // arrange
            EmailDispatchException exception = mock(EmailDispatchException.class);
            EmailDispatchParam param = magicCodeDispatchParam();
            // conditions
            stubRow();
            when(exception.isRetryable()).thenReturn(false);
            when(inboxStore.toDispatchParam(row)).thenReturn(param);
            doThrow(exception).when(emailDispatcher).dispatch(param);
            // act
            processor.process(row);
            // assert
            // verify
            verify(inboxStore).markPermanentFailure(EVENT_ID, exception);
        }

        @Test
        @DisplayName("marks unexpected exception for retry")
        void marksUnexpectedExceptionForRetry() {
            // arrange
            RuntimeException exception = new RuntimeException("unexpected");
            EmailDispatchParam param = magicCodeDispatchParam();
            // conditions
            stubRow();
            when(inboxStore.toDispatchParam(row)).thenReturn(param);
            doThrow(exception).when(emailDispatcher).dispatch(param);
            // act
            processor.process(row);
            // assert
            // verify
            verify(inboxStore).markRetryableFailure(EVENT_ID, exception);
        }

        private void stubRow() {
            when(row.getEventId()).thenReturn(EVENT_ID);
            when(row.getEventType()).thenReturn(EVENT_TYPE);
        }

    }

}
