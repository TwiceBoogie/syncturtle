package com.syncturtle.services.email.service.result;

import org.springframework.util.Assert;

import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.type.EmailInboxAcquireDecision;

import lombok.Getter;

@Getter
public final class EmailInboxAcquireResult {

    private final EmailInboxAcquireDecision decision;
    private final EmailEventInbox row;

    private EmailInboxAcquireResult(EmailInboxAcquireDecision decision, EmailEventInbox row) {
        Assert.notNull(decision, "decision is required");
        Assert.notNull(row, "email inbox row is required");

        this.decision = decision;
        this.row = row;
    }

    public static EmailInboxAcquireResult acquired(EmailEventInbox row) {
        return new EmailInboxAcquireResult(EmailInboxAcquireDecision.ACQUIRED, row);
    }

    public static EmailInboxAcquireResult alreadySent(EmailEventInbox row) {
        return new EmailInboxAcquireResult(EmailInboxAcquireDecision.ALREADY_SENT, row);
    }

    public static EmailInboxAcquireResult inProgress(EmailEventInbox row) {
        return new EmailInboxAcquireResult(EmailInboxAcquireDecision.IN_PROGRESS, row);
    }

    public static EmailInboxAcquireResult retryScheduled(EmailEventInbox row) {
        return new EmailInboxAcquireResult(EmailInboxAcquireDecision.RETRY_SCHEDULED, row);
    }

    public static EmailInboxAcquireResult permanentFailure(EmailEventInbox row) {
        return new EmailInboxAcquireResult(EmailInboxAcquireDecision.PERMANENT_FAILURE, row);
    }

}
