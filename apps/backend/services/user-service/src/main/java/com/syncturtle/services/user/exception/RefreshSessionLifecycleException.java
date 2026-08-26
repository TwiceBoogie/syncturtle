package com.syncturtle.services.user.exception;

import lombok.Getter;

@Getter
public final class RefreshSessionLifecycleException extends RuntimeException {

    public enum Reason {
        MISSING_FAMILY,
        EXPIRED_FAMILY,
        REPLAY_REVOKED,
        MALFORMED_STATE,
        WRONG_REDIS_TYPE,
        UNSUPPORTED_RECORD_VERSION,
        ENVELOPE_FAILURE,
        REDIS_FAILURE,
        INVARIANT_VIOLATION
    }

    private final Reason reason;

    public RefreshSessionLifecycleException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public RefreshSessionLifecycleException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

}
