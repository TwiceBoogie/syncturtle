package com.syncturtle.platform.gateway.exception;

public final class GatewayCsrfTokenException extends RuntimeException {

    public GatewayCsrfTokenException() {
        super("Invalid CSRF token");
    }

}
