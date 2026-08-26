package com.syncturtle.services.email.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.syncturtle.services.email.service.EmailInboxService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.email.inbox.scheduler", name = "enabled", havingValue = "true")
public class EmailRetryScheduler {

    private final EmailInboxService service;

    @Scheduled(fixedDelayString = "${app.email.inbox.scheduler.poll-delay}")
    public void retryDueEmails() {
        service.retryDueEmails();
    }

}
