package com.syncturtle.services.email.enums;

public enum EmailEventInboxStatus {
    PROCESSING,
    SENT,
    FAILED_RETRYABLE,
    FAILED_PERMANENT
}
