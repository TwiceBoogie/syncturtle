package com.syncturtle.services.email.service;

import org.springframework.stereotype.Service;

import com.syncturtle.services.email.dto.EmailEnvelope;
import com.syncturtle.services.email.exception.EmailDispatchException;
import com.syncturtle.services.email.exception.EmailInboxException;
import com.syncturtle.services.email.models.EmailEventInbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailInboxProcessor {

    private final EmailDispatchService emailDispatchService;
    private final EmailEventInboxService emailEventInboxService;

    public void process(EmailEventInbox row) {
        EmailEnvelope envelope;

        try {
            envelope = emailEventInboxService.toEnvelope(row);
        } catch (EmailInboxException exception) {
            emailEventInboxService.markPermanentFailure(row.getEventId(), exception);

            log.error(
                    "Email inbox payload failed permanently. eventId={}, correlationId={}",
                    row.getEventId(),
                    row.getCorrelationId(),
                    exception);

            return;
        }

        try {
            emailDispatchService.send(envelope);
            emailEventInboxService.markSent(row.getEventId());

            log.info("Email inbox row sent successfully. eventId={}, correlationId={}, attemptCount={}",
                    row.getEventId(),
                    row.getCorrelationId(),
                    row.getAttemptCount());
        } catch (EmailDispatchException exception) {
            if (exception.isRetryable()) {
                emailEventInboxService.markRetryableFailure(row.getEventId(), exception);
                log.warn("Email send failed and was scheduled for retry. eventId={}, correlationId={}",
                        row.getEventId(),
                        row.getCorrelationId(),
                        exception);
                return;
            }

            emailEventInboxService.markPermanentFailure(row.getEventId(), exception);
            log.error("Email send failed permanently. eventId={}, correlationId={}",
                    row.getEventId(),
                    row.getCorrelationId(),
                    exception);
        } catch (Exception exception) {
            emailEventInboxService.markRetryableFailure(row.getEventId(), exception);
            log.warn("Unexpected email send failure was scheduled for retry. eventId={}, correlationId={}",
                    row.getEventId(),
                    row.getCorrelationId(),
                    exception);
        }
    }

}
