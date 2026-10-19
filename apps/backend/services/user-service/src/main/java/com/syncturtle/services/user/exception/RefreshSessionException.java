package com.syncturtle.services.user.exception;

import lombok.Getter;

@Getter
public final class RefreshSessionException extends RuntimeException {

    public enum Reason {
        INVALID, STALE, UNAVAILABLE
    }

    private final Reason reason;

    public RefreshSessionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public RefreshSessionException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

}
