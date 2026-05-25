package com.syncturtle.services.email.unit.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.dto.EmailEnvelope;
import com.syncturtle.services.email.exceptions.EmailDispatchException;
import com.syncturtle.services.email.exceptions.EmailInboxException;
import com.syncturtle.services.email.models.EmailEventInbox;
import com.syncturtle.services.email.service.EmailDispatchService;
import com.syncturtle.services.email.service.EmailEventInboxService;
import com.syncturtle.services.email.service.EmailInboxProcessor;

import jakarta.mail.SendFailedException;

@ExtendWith(MockitoExtension.class)
class EmailInboxProcessorTest {

    @Mock
    EmailDispatchService emailDispatchService;

    @Mock
    EmailEventInboxService emailEventInboxService;

    @InjectMocks
    EmailInboxProcessor processor;

    @Test
    void process_whenEnvelopeRebuildFails_marksPermanentFailureAndDoesNotDispatch() {
        // arrange
        EmailEventInbox row = row();
        EmailInboxException exception = EmailInboxException.payloadDeserializationFailed(
                "evt-123",
                new IllegalArgumentException("bad json"));

        when(emailEventInboxService.toEnvelope(row)).thenThrow(exception);
        // act
        processor.process(row);
        // assert / verify
        verify(emailEventInboxService).toEnvelope(row);
        verify(emailEventInboxService).markPermanentFailure("evt-123", exception);
        verify(emailEventInboxService, never()).markSent("evt-123");
        verify(emailEventInboxService, never()).markRetryableFailure(eq("evt-123"), any());
        verifyNoInteractions(emailDispatchService);
    }

    @Test
    void process_whenDispatchSucceeds_marksSent() {
        // arrange
        EmailEventInbox row = row();
        EmailEnvelope envelope = envelope();

        when(emailEventInboxService.toEnvelope(row)).thenReturn(envelope);
        // act
        processor.process(row);
        // assert / verify
        verify(emailDispatchService).send(envelope);
        verify(emailEventInboxService).markSent("evt-123");
        verify(emailEventInboxService, never()).markRetryableFailure(eq("evt-123"), any());
        verify(emailEventInboxService, never()).markPermanentFailure(eq("evt-123"), any());
    }

    @Test
    void process_whenDispatchThrowsRetryable_marksRetryableFailure() {
        // arrange
        EmailEventInbox row = row();
        EmailEnvelope envelope = envelope();
        EmailDispatchException exception = EmailDispatchException.sendFailed(
                new IllegalStateException("smtp unavailable"));

        when(emailEventInboxService.toEnvelope(row)).thenReturn(envelope);
        doThrow(exception).when(emailDispatchService).send(envelope);
        // act
        processor.process(row);
        // assert / verify
        verify(emailEventInboxService).markRetryableFailure("evt-123", exception);
        verify(emailEventInboxService, never()).markSent("evt-123");
        verify(emailEventInboxService, never()).markPermanentFailure(eq("evt-123"), any());
    }

    @Test
    void process_whenDispatchThrowsPermanent_marskPermanentFailure() {
        // arrange
        EmailEventInbox row = row();
        EmailEnvelope envelope = envelope();
        EmailDispatchException exception = EmailDispatchException.recipientsRefused(
                new SendFailedException("all recipients refused"));

        when(emailEventInboxService.toEnvelope(row)).thenReturn(envelope);
        doThrow(exception).when(emailDispatchService).send(envelope);
        // act
        processor.process(row);
        // assert / verify
        verify(emailEventInboxService).markPermanentFailure("evt-123", exception);
        verify(emailEventInboxService, never()).markSent("evt-123");
        verify(emailEventInboxService, never()).markRetryableFailure(eq("evt-123"), any());
    }

    @Test
    void process_whenUnexpectedExceptionDuringDispatch_marksRetryableFailure() {
        // arrange
        EmailEventInbox row = row();
        EmailEnvelope envelope = envelope();
        RuntimeException exception = new RuntimeException("error");

        when(emailEventInboxService.toEnvelope(row)).thenReturn(envelope);
        doThrow(exception).when(emailDispatchService).send(envelope);
        // act
        processor.process(row);
        // assert
        // verify
        verify(emailEventInboxService).markRetryableFailure("evt-123", exception);
        verify(emailEventInboxService, never()).markSent("evt-123");
        verify(emailEventInboxService, never()).markPermanentFailure(eq("evt-123"), any());
    }

    private static EmailEventInbox row() {
        EmailEventInbox row = new EmailEventInbox();
        row.setEventId("evt-123");
        row.setCorrelationId("corr-123");
        row.setAttemptCount(1);
        return row;
    }

    private static EmailEnvelope envelope() {
        return EmailEnvelope.builder()
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject("Your magic link")
                .to(List.of("user@example.com"))
                .model(Map.of("magicLink", "https://app.syncturtle.com/magic/?token=abc123"))
                .correlationId("corr-123")
                .build();
    }

}
