package com.syncturtle.services.email.service;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;

/**
 * Coordinates durable intake and retry processing for email events.
 * 
 * <p>
 * <b>Note:</b>
 * A newly received event is first inserted into the inbox. The db unique
 * constraint on the event identifier acts as the final idempotency guard. When
 * dupe exception is thrown, the existing row is locked and resolved to a
 * decision
 * for processing
 * </p>
 */
public interface EmailInboxService {
    /**
     * Accepts one event into the durable inbox and processes it only only when this
     * consumer acquires ownership.
     * 
     * @param event email request received from Kafka
     */
    void receive(EmailToSendEvent event);

    /**
     * Claims a bounded batch of retryable or abandoned rows and processes each
     * acquired row.
     * 
     * <p>
     * Claiming occurs transactionally inside {@code EmailInboxStore}; processing
     * occurs afterward so the db locks are not held during SMTP calls.
     */
    void retryDueEmails();
}
