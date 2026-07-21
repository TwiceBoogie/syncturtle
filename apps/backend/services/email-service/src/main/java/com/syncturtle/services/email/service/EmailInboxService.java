package com.syncturtle.services.email.service;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;

public interface EmailInboxService {
    void receive(EmailToSendEvent event);

    void retryDueEmails();
}
