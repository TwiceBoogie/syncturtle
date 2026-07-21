package com.syncturtle.services.email.service.impl;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.services.email.configuration.property.EmailInboxProperties;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.service.EmailInboxService;
import com.syncturtle.services.email.service.inbox.EmailInboxProcessor;
import com.syncturtle.services.email.service.inbox.EmailInboxStore;
import com.syncturtle.services.email.service.result.EmailInboxAcquireResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailInboxServiceImpl implements EmailInboxService {

    private final EmailInboxStore inboxStore;
    private final EmailInboxProcessor inboxProcessor;
    private final EmailInboxProperties properties;

    @Override
    public void receive(EmailToSendEvent event) {
        Assert.notNull(event, "email event is required");

        EmailInboxAcquireResult result;

        try {
            result = inboxStore.insertFresh(event);
        } catch (DataIntegrityViolationException exception) {
            result = inboxStore.resolveExisting(event.getEventId());
        }

        switch (result.getDecision()) {
            case ALREADY_SENT ->
                log.info("Skipping dupe email event already marked SENT. eventId={}", event.getEventId());
            case IN_PROGRESS ->
                log.info("Skipping email event already being processed. eventId={}", event.getEventId());
            case RETRY_SCHEDULED ->
                log.info("Skipping dupe email event already scheduled for retry. eventId={}", event.getEventId());
            case PERMANENT_FAILURE ->
                log.warn("Skipping dupe email event already marked permanently failed. eventId={}", event.getEventId());
            case ACQUIRED -> inboxProcessor.process(result.getRow());
        }
    }

    @Override
    public void retryDueEmails() {
        int batchSize = properties.getProcessing().getBatchSize();
        List<EmailEventInbox> batch = inboxStore.acquireDueRetries(batchSize);

        if (batch.isEmpty()) {
            return;
        }

        log.info("Claimed {} email inbox rows for retry processing.", batch.size());

        for (EmailEventInbox row : batch) {
            inboxProcessor.process(row);
        }
    }

}
