package com.syncturtle.services.email.type;

public enum EmailEventInboxStatus {
    PROCESSING,
    SENT,
    FAILED_RETRYABLE,
    FAILED_PERMANENT
}
