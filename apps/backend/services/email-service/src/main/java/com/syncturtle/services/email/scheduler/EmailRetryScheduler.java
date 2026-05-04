package com.syncturtle.services.email.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.syncturtle.services.email.models.EmailEventInbox;
import com.syncturtle.services.email.service.EmailEventInboxService;
import com.syncturtle.services.email.service.EmailInboxProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.email.retry.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailRetryScheduler {

    private final EmailEventInboxService emailEventInboxService;
    private final EmailInboxProcessor emailInboxProcessor;

    @Value("${app.email.retry.batch-size:25}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.email.retry.scheduler.poll-delay-ms:10000}")
    public void retryDueEmails() {
        List<EmailEventInbox> batch = emailEventInboxService.acquireDueRetries(batchSize);

        if (batch.isEmpty()) {
            return;
        }

        log.info("Claimed {} email inbox rows for retry processing.", batch.size());

        for (EmailEventInbox row : batch) {
            emailInboxProcessor.process(row);
        }
    }

}
