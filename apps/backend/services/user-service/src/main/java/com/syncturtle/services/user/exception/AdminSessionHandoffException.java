package com.syncturtle.services.user.exception;

import java.util.Objects;

import lombok.Getter;

@Getter
public final class AdminSessionHandoffException extends RuntimeException {

    public enum Reason {
        INVALID,
        EXPIRED,
        REPLAYED,
        BINDING_MISMATCH,
        MALFORMED_RECORD,
        UNSUPPORTED_RECORD_VERSION,
        WRONG_REDIS_TYPE,
        REDIS_UNAVAILABLE,
        INVARIANT_VIOLATION
    }

    private final Reason reason;

    public AdminSessionHandoffException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason is required");
    }

    public AdminSessionHandoffException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason is required");
    }

}
