package com.syncturtle.services.email.service.inbox;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.services.email.exception.EmailDispatchException;
import com.syncturtle.services.email.exception.EmailInboxException;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.service.dispatch.EmailDispatcher;
import com.syncturtle.services.email.service.param.EmailDispatchParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailInboxProcessor {

    private final EmailInboxStore inboxStore;
    private final EmailDispatcher emailDispatcher;

    public void process(EmailEventInbox row) {
        Assert.notNull(row, "email inbox row is required");

        EmailDispatchParam dispatchParam;

        try {
            dispatchParam = inboxStore.toDispatchParam(row);
        } catch (EmailInboxException exception) {
            inboxStore.markPermanentFailure(row.getEventId(), exception);

            log.warn("Email inbox payload failed permanently. eventId={}, eventType={}", row.getEventId(),
                    row.getEventType(), exception);

            return;
        }

        try {
            emailDispatcher.dispatch(dispatchParam);

            inboxStore.markSent(row.getEventId());

            log.info("Email inbox row sent successfully. eventId={}, eventType={}, attemptCount={}", row.getEventId(),
                    row.getEventType(), row.getAttemptCount());
        } catch (EmailDispatchException exception) {
            if (exception.isRetryable()) {
                inboxStore.markRetryableFailure(row.getEventId(), exception);

                log.warn("Email send failed and was scheduled for retry. eventId={}, eventType={}", row.getEventId(),
                        row.getEventType(), exception);

                return;
            }

            inboxStore.markPermanentFailure(row.getEventId(), exception);

            log.warn("Email send failed permanently. eventId={}, eventType={}", row.getEventId(), row.getEventType(),
                    exception);
        } catch (Exception exception) {
            inboxStore.markRetryableFailure(row.getEventId(), exception);

            log.warn("Unexpected email send failure was scheduled for retry. eventId={}, eventType={}",
                    row.getEventId(), row.getEventType(), exception);
        }
    }

}
