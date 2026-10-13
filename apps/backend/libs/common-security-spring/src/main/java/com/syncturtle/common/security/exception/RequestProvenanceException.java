package com.syncturtle.common.security.exception;

public final class RequestProvenanceException extends RuntimeException {

    public RequestProvenanceException(String message) {
        super(message);
    }

    public RequestProvenanceException(String message, Throwable cause) {
        super(message, cause);
    }

}
