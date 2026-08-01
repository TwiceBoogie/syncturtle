package com.syncturtle.services.email.type;

public enum EmailInboxAcquireDecision {
    ACQUIRED,
    ALREADY_SENT,
    IN_PROGRESS,
    RETRY_SCHEDULED,
    PERMANENT_FAILURE
}
