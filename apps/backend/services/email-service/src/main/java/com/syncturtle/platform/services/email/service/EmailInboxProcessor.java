package com.syncturtle.platform.services.email.service;

import org.springframework.stereotype.Service;

import com.syncturtle.platform.services.email.exceptions.EmailDispatchException;
import com.syncturtle.platform.services.email.models.EmailEventInbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailInboxProcessor {

    private final EmailDispatchService emailDispatchService;
    private final EmailEventInboxService emailEventInboxService;

    public void process(EmailEventInbox row) {
        try {
            emailDispatchService.send(emailEventInboxService.toEnvelope(row));
            emailEventInboxService.markSent(row.getEventId());

            log.info("Email inbox row sent successfully. eventId={}, correlationId={}, attemptCount={}",
                    row.getEventId(), row.getCorrelationId(), row.getAttemptCount());
        } catch (EmailDispatchException exception) {
            if (exception.isRetryable()) {
                emailEventInboxService.markRetryableFailure(row.getEventId(), exception);
                log.warn("Email send failed and was scheduled for retry. eventId={}, correlationId={}",
                        row.getEventId(), row.getCorrelationId(), exception);
                return;
            }

            emailEventInboxService.markPermanentFailure(row.getEventId(), exception);
            log.error("Email send failed permanently. eventId={}, correlationId={}", row.getEventId(),
                    row.getCorrelationId(), exception);
        } catch (Exception exception) {
            emailEventInboxService.markRetryableFailure(row.getEventId(), exception);
            log.warn("Unexpected email send failure was scheduled for retry. eventId={}, correlationId={}",
                    row.getEventId(), row.getCorrelationId(), exception);
        }
    }

}
