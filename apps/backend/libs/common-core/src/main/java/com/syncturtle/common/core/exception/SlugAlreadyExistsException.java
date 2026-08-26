package com.syncturtle.common.core.exception;

public final class SlugAlreadyExistsException extends RuntimeException {

    public SlugAlreadyExistsException(String message) {
        super(message);
    }

    public SlugAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}
