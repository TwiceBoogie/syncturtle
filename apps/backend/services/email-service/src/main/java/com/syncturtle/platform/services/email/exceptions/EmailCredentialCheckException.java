package com.syncturtle.platform.services.email.exceptions;

import org.springframework.http.HttpStatus;

public class EmailCredentialCheckException extends RuntimeException {

    private final HttpStatus status;

    public EmailCredentialCheckException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public EmailCredentialCheckException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
