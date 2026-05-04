package com.syncturtle.services.email.exceptions;

public class EmailDispatchException extends RuntimeException {

    private final boolean retryable;

    private EmailDispatchException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public static EmailDispatchException retryable(String message, Throwable cause) {
        return new EmailDispatchException(message, true, cause);
    }

    public static EmailDispatchException permanent(String message, Throwable cause) {
        return new EmailDispatchException(message, false, cause);
    }

}
