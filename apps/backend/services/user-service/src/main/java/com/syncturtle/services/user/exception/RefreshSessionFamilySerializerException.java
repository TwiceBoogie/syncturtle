package com.syncturtle.services.user.exception;

import java.util.Objects;

import lombok.Getter;

@Getter
public final class RefreshSessionFamilySerializerException extends IllegalStateException {

    public enum Reason {
        MISSING_RECORD_VERSION,
        MALFORMED_RECORD_VERSION,
        UNSUPPORTED_RECORD_VERSION,
        MALFORMED_RECORD
    }

    private final Reason reason;

    public RefreshSessionFamilySerializerException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason is required");
    }

    public RefreshSessionFamilySerializerException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason is required");
    }

}
